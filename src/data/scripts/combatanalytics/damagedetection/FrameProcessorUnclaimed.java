package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import data.scripts.combatanalytics.util.Helpers;
import data.scripts.combatanalytics.util.VectorUtil;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static data.scripts.combatanalytics.util.Helpers.formatAsPercent;

public class FrameProcessorUnclaimed extends FrameProcessor {
    private static final float PRIOR_FRAME_LIFETIME_AMOUNT = .5F;

    // explosions have a larger radius than this, but too large and it'll result in false-positives.
    // this value encompasses about 90% of the damages
    private static final double MAX_EXPLOSION_DISTANCE_FOR_CONSIDERATION = 75d;

    // all of the projectile damages that happened this battle
    private final List<ReportableDamage> _unclaimedDamages = new ArrayList<>(1000);

    private double _lifetime = 0;

    private int _processedCount = 0;
    private int _wasCustom = 0;
    private int _wasUnknown = 0;
    private int _collisions = 0;

    private double _accurateDamage;
    private double _unknownDamage;

    protected FrameProcessorUnclaimed(CombatEngineAPI engine, FrameProcessorState state, ListenerManager listenerManager) {
        super(engine, state, listenerManager);
    }

    @Override
    public void internalProcessFrame(float amount) {
        _lifetime += amount;

        List<ListenerDamage> maybeCollisions = new ArrayList<>();
        for(Map.Entry<SourceTargetDamageType, List<ListenerDamage>> entry : _listenerManager.getListenerDamages(false).entrySet()){
            for (ListenerDamage damage : entry.getValue()){

                String weaponName = getWeaponName(damage, maybeCollisions);
                if(weaponName != null){
                    _unclaimedDamages.add(new ReportableDamage(weaponName, damage, wasKillingBlow(damage, weaponName)));
                } else if(_state.isShipExploding(damage.source)){
                    //todo to properly handle ships that are suicide ships (they just explode)
                    // it's made difficult because all ships explode when they are logging damages (usually 8 or more).
                    // We don't want to track that damage because it's not weapon related and just clutters things
                }
            }
        }

        // check for collisions
        HashSet<ListenerDamage> matched = new HashSet<>();
        for(ListenerDamage ld1 : maybeCollisions){
            if(matched.contains(ld1)){
                continue;
            }

            for(ListenerDamage ld2 : maybeCollisions){
                if(ld2.target == ld1.source
                && ld2.source == ld1.target){
                    matched.add(ld1);
                    matched.add(ld2);

                    _unclaimedDamages.add(new ReportableDamage(Helpers.getCollisionWeaponName(ld1.damageType), ld1, wasKillingBlow(ld1, Helpers.getCollisionWeaponName(ld1.damageType))));
                    _unclaimedDamages.add(new ReportableDamage(Helpers.getCollisionWeaponName(ld2.damageType), ld2, wasKillingBlow(ld2, Helpers.getCollisionWeaponName(ld2.damageType))));

                    _collisions += 2;

                    break;
                }
            }

            if(!matched.contains(ld1)){
                _wasUnknown ++;
                _unknownDamage += ld1.getListedDamage();
                _unclaimedDamages.add(new ReportableDamage(Helpers.getUnknownWeaponName(ld1.damageType), ld1, wasKillingBlow(ld1, Helpers.getUnknownWeaponName(ld1.damageType))));
            }
        }
    }

    private boolean isValidForProcessing(ShipAPI source, ShipAPI target){
        // standard rules for damage for catching weapon damages like mines
        return (source.isAlive() || _state.aliveShipsLastFrameById.containsKey(source.getId()))
               && (target.isAlive() || _state.aliveShipsLastFrameById.containsKey(target.getId()));
    }

    private boolean isValidForProcessingStrict(ShipAPI source, ShipAPI target){
        // lots of the damages that make it here are the result of ships exploding on death.  So be strict, both ships
        // MUST be alive this frame for damage to count
        return source.isAlive() && target.isAlive();
    }

    public String getWeaponName(ListenerDamage ld, List<ListenerDamage> maybeCollisions){
        if(!isValidForProcessing(ld.source, ld.target)){
            return null;
        }
        _processedCount++;

        // special cases are weapons and will be processed with weapon strictness
        String weaponName = processSpecialCase(ld);
        if(weaponName != null){
            return weaponName;
        }

        // collisions and "unknown" are only allowed for those that pass the strict test
        if(!isValidForProcessingStrict(ld.source, ld.target)){
            return null;
        }

        if(areShipsMaybeColliding(ld.source, ld.target)){
            maybeCollisions.add(ld);
            return null;
        }

        _wasUnknown++;
        _unknownDamage += ld.getListedDamage();

        return Helpers.getUnknownWeaponName(ld.damageType);
    }

    private String processSpecialCase(ListenerDamage ld){
        ShipHullSpecAPI hullSpec = ld.source.getHullSpec();
        String hullId = hullSpec.getBaseHullId() == null ? hullSpec.getHullId() : hullSpec.getBaseHullId();

        // Hack, but it works.  People expect the coolest ship in the game to have sort of well organized damage data
        if(hullId.equalsIgnoreCase("ziggurat") && ld.damageType == DamageType.ENERGY
        ){ // just assume it's a mote, there's so much crazy that can happen with mote on-hit effects this will capture them all
            _accurateDamage += ld.getListedDamage();
            _wasCustom++;
            return "Mote";
        }

        return null;
    }

    public boolean areShipsMaybeColliding(ShipAPI source, ShipAPI target){
        Vector2f a = source.getLocation();
        Vector2f b = target.getLocation();

        return VectorUtil.distance(a, b) < 400;
    }

    public String getStatsToString(){
        String ret = "Unclaimed Processor ("+getProcessingTime()+"ms):  Total: " + _processedCount;

        if(_processedCount > 0) {
            ret +=  "  Custom: " + _wasCustom + " (" + formatAsPercent(_wasCustom / (double) _processedCount) + ")" +
                    "  Collision: " + _collisions +
                    "  Unknown " + _wasUnknown + " (" + formatAsPercent(_wasUnknown / (double) _processedCount) + ")";
        }

        return ret;
    }

    public Collection<ReportableDamage> getCombatDamages() {
        return _unclaimedDamages;
    }

    private static class Explosion {
        public final float created;
        public final DamagingProjectileAPI projectileAPI;

        public Explosion(DamagingProjectileAPI projectileAPI, float created){
            this.projectileAPI = projectileAPI;
            this.created = created;
        }

        @Override
        public String toString() {
            return "{" +
                    "x=" + projectileAPI.getLocation().x +
                    ", y=" + projectileAPI.getLocation().y +
                    " DT="+projectileAPI.getDamageType() +
                    '}';
        }
    }
}
