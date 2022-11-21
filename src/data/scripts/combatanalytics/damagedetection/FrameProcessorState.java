package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.combat.entities.DamagingExplosion;
import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.data.ShipStatus;
import data.scripts.combatanalytics.util.Helpers;
import data.scripts.combatanalytics.util.VectorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static data.scripts.combatanalytics.util.Constants.MINE;

// shared state among processing objects
public class FrameProcessorState {
    private static final float PROJECTILE_KEEP_ALIVE = 1.0f;

    // ships we've seen, intern to reduce memory
    final HashMap<String, Ship> shipIdToTrackedShip = new HashMap<>(100);

    // ships that were alive last frame.
    HashMap<String, ShipAPI> aliveShipsLastFrameById = new HashMap<>(100);
    HashMap<String, ShipAPI> aliveShipsThisFrameById = new HashMap<>(100);
    HashMap<String, ShipAPI> killedShipsThisFrameById = new HashMap<>(100);

    HashMap<DamagingExplosion, String> explosionToCause = new HashMap<>(100);

    private HashMap<DamagingProjectileAPI, Double> historicalProjectilesToAge = new HashMap<>();

    double currentAge = 0;

    private long _processingTime = 0;
    private int _identifiedExplosions = 0;

    private final String _combatId;

    public FrameProcessorState(String combatId){
        _combatId = combatId;
    }

    public void updateCommonState(float amount, CombatEngineAPI engine){
        long start = System.currentTimeMillis();

        currentAge += amount;

        // look for new explosions, find their source
        for(DamagingProjectileAPI projectile : engine.getProjectiles()){
            if(projectile instanceof DamagingExplosion){
                DamagingExplosion de = (DamagingExplosion) projectile;
                if(!explosionToCause.containsKey(de)){
                    explosionToCause.put(de, determineExplosionCause(de, engine));
                }
            }
        }

        // expire old explosions
        for(DamagingExplosion de : new ArrayList<>(explosionToCause.keySet())){
            if(de.isExpired()){
                explosionToCause.remove(de);
            }
        }

        // track all historical projectiles for up to a second after they are gone.
        HashMap<DamagingProjectileAPI, Double> newHistoricalProjectiles = new HashMap<>(historicalProjectilesToAge.size() * 2);
        for(Map.Entry<DamagingProjectileAPI, Double> projAndFrame : historicalProjectilesToAge.entrySet()){
            if(!projAndFrame.getKey().isExpired() && projAndFrame.getValue() > currentAge){
                newHistoricalProjectiles.put(projAndFrame.getKey(), projAndFrame.getValue());
            }
        }

        for(DamagingProjectileAPI projectile : engine.getProjectiles()){
            newHistoricalProjectiles.put(projectile, currentAge + 1); // expire time is 1s in the future
        }
        historicalProjectilesToAge = newHistoricalProjectiles;

        aliveShipsLastFrameById = aliveShipsThisFrameById;
        killedShipsThisFrameById = new HashMap<>(aliveShipsLastFrameById);
        aliveShipsThisFrameById = new HashMap<>(aliveShipsLastFrameById.size() * 2);
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isAlive()) {
                aliveShipsThisFrameById.put(ship.getId(), ship);
                killedShipsThisFrameById.remove(ship.getId());
            }
        }

        _processingTime += System.currentTimeMillis() - start;
    }

    public boolean isShipExploding(ShipAPI ship){
        return !ship.isAlive() && aliveShipsLastFrameById.containsKey(ship.getId());
    }

    // intern our TrackedShips, warehouse pattern
    Ship getTrackedShip(ShipAPI ship) {
        if(ship.isStationModule()){ // get base ship for module ships
            ship = ship.getParentStation();
        }

        // this is the ShipAPI.id not Ship.id!
        String id = ship.getId();

        Ship ret = shipIdToTrackedShip.get(id);
        if (ret == null) {
            ret = new Ship(ship, _combatId);
            ret.setCaptain(ship.getCaptain()); // track here so it shows up in Simulation Results
            shipIdToTrackedShip.put(id, ret);
        }

        return ret;
    }

    Ship getFakeShipForMissile(String missileName, int owner) {
        // this is the ShipAPI.id not Ship.id!
        String id = Helpers.getSmallUuid()+"m";

        Ship ret = new Ship(
                id,
                _combatId,
                missileName,
                ShipAPI.HullSize.DEFAULT,
                missileName,
                1,
                1,
                owner,
                0,
                Ship.NO_CAPTAIN,
                "missile",
                ShipStatus.DESTROYED,
                "",
                0
        );

        shipIdToTrackedShip.put(id, ret);
        return ret;
    }

    public String getExplosionCause(DamagingExplosion de){
        if(explosionToCause.containsKey(de)){
            return explosionToCause.get(de);
        } else {
            return "Unknown Explosion";
        }
    }

    private String determineExplosionCause(DamagingExplosion explosion, CombatEngineAPI engine){
        _identifiedExplosions++;

        // check projectiles
        DamagingProjectileAPI winner = null;
        double winnerDistance = 999999999999999d;
        for(DamagingProjectileAPI potentialCause : Helpers.concat(historicalProjectilesToAge.keySet(), engine.getProjectiles())){
            if(potentialCause instanceof DamagingExplosion){ // explosions aren't caused by explosions
                continue;
            }

            if(potentialCause.getSource() != explosion.getSource()){
                continue;
            }

            double thisDistance = VectorUtil.distance(explosion.getLocation(), potentialCause.getLocation());
            if(thisDistance < winnerDistance){
                winnerDistance = thisDistance;
                winner = potentialCause;

                if(winnerDistance == 0){
                    break;
                }
            }
        }

        //todo beams?  Maybe?  they don't usually cause explosions
        // and when they do those explosions are mines and are tracked already, usually as a mine...

        // the distance is almost always 0
        if(winner != null && winnerDistance < 25){
            if(winner.getWeapon() == null){
                if(winner instanceof MissileAPI && ((MissileAPI)winner).isMine()){
                    return MINE;
                }
            } else {
                return winner.getWeapon().getDisplayName();
            }
        } else {
            for(ShipAPI ship : this.aliveShipsLastFrameById.values()){
                if(VectorUtil.distance(explosion.getLocation(), ship.getLocation()) < 25){
                    return "Ship Explosion";
                }
            }
        }

        // can't get explosion spec to check and see which weapons match its damage.  Probably a ship system anyway.
        return Helpers.getUnknownWeaponName(explosion.getDamageType());
    }

    public String getStatsToString(){
        return "FrameProcessorState ("+_processingTime+"ms):   Identified Explosions: " + _identifiedExplosions;
    }
}
