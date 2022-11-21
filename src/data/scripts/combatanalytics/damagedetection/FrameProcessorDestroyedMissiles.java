package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.combat.entities.DamagingExplosion;
import data.scripts.combatanalytics.util.Helpers;
import data.scripts.combatanalytics.util.VectorUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Watches every frame for destroyed missiles, tries to figure out who destroyed them
 */
public class FrameProcessorDestroyedMissiles extends FrameProcessor  {
    private HashSet<DamagingProjectileAPI> _missilesActiveLastFrame = new HashSet<>();
    private HashSet<DamagingProjectileAPI> _ballisticsActiveLastFrame = new HashSet<>();

    private final List<InterceptedMissile> _interceptedMissiles = new ArrayList<>();
    private int _trackedMissiles = 0;

    protected FrameProcessorDestroyedMissiles(CombatEngineAPI engine, FrameProcessorState state, ListenerManager listenerManager) {
        super(engine, state, listenerManager);
    }

    public void internalProcessFrame(float amount) {
        HashSet<DamagingProjectileAPI> missilesActiveThisFrame = getActiveMissiles();

        // find all missiles that died
        _missilesActiveLastFrame.removeAll(missilesActiveThisFrame);
        _trackedMissiles += _missilesActiveLastFrame.size();

        // try to figure out what killed them
        for(DamagingProjectileAPI deadMissile : _missilesActiveLastFrame){
            if(deadMissile.didDamage()){ // not intercepted
                continue;
            }
            RawDamage missileDamage = new RawDamage(deadMissile.getDamage().getBaseDamage(), deadMissile.getDamage().getFluxComponent(), deadMissile.getDamage().getType());
            MissileKiller interceptor = determineInterceptor((MissileAPI) deadMissile);
            if(interceptor == null){
                continue;
            }

            String deadMissileName = deadMissile.getWeapon() == null ? "Missile" : deadMissile.getWeapon().getDisplayName();

            RawDamage interceptorDamage = new RawDamage(interceptor.getDamage().getBaseDamage(), interceptor.getDamage().getFluxComponent(), interceptor.getDamage().getType());
            String weaponName = interceptor.isExplosion() ? _state.getExplosionCause(interceptor.getExplosion()) : interceptor.getWeaponName();
            InterceptedMissile im = new InterceptedMissile(deadMissile.getSource(), missileDamage, deadMissileName, interceptor.getSource(), weaponName, interceptorDamage);

            _interceptedMissiles.add(im);
        }

        _ballisticsActiveLastFrame = getActiveBallistics();
        _missilesActiveLastFrame = missilesActiveThisFrame;
    }

    private HashSet<DamagingProjectileAPI> getActiveMissiles(){
        HashSet<DamagingProjectileAPI> ret = new HashSet<>(100);
        for (DamagingProjectileAPI proj : _engine.getProjectiles()) {
            if (proj instanceof MissileAPI) {  // missiles are handled differently
                ret.add(proj);
            }
        }

        return ret;
    }

    // get the things that aren't missiles or explosions (can be weird stuff like plasma or flak)
    private HashSet<DamagingProjectileAPI> getActiveBallistics(){
        HashSet<DamagingProjectileAPI> ret = new HashSet<>(100);
        for (DamagingProjectileAPI proj : _engine.getProjectiles()) {
            if (!(proj instanceof MissileAPI)
                && !(proj instanceof DamagingExplosion)
            ) {
                ret.add(proj);
            }
        }

        return ret;
    }

    /**
     * There's an explosion, see if we can figure out what weapon created it based on what projectiles or beams were around last frame
     */
    private MissileKiller determineInterceptor(MissileAPI deadMissile){
        // check projectiles
        DamagingProjectileAPI winner = null;
        double winnerDistance = 999999999999999d;
        for(DamagingProjectileAPI interceptor : Helpers.concat(_missilesActiveLastFrame, _ballisticsActiveLastFrame, _engine.getProjectiles())){
            if(interceptor == deadMissile || interceptor.getSource() == deadMissile.getSource() || interceptor.getOwner() == deadMissile.getOwner()){
                continue;
            }

            // if it's caught in an explosion, assume the explosion did it
            if(interceptor instanceof DamagingExplosion){
                DamagingExplosion explosion = (DamagingExplosion) interceptor;
                for(CombatEntityAPI da : explosion.getDamagedAlready()){
                    if(da.equals(deadMissile)){
                        return new MissileKiller(explosion, null, interceptor.getDamage(), interceptor.getSource());
                    }
                }
            }

            double thisDistance = VectorUtil.distance(deadMissile.getLocation(), interceptor.getLocation());
            if(thisDistance < winnerDistance){
                winnerDistance = thisDistance;
                winner = interceptor;
            }
        }

        for(BeamAPI beam : _engine.getBeams()){
            if(beam.getDamageTarget() == deadMissile){
                return new MissileKiller(beam, beam.getWeapon(), beam.getDamage(), beam.getSource());
            }
        }

        if(winner == null || winnerDistance > 20){
            return null; // nothing intercepted it, just died a natural death
        }

        return new MissileKiller(winner, winner.getWeapon(), winner.getDamage(), winner.getSource());
    }


    public List<InterceptedMissile> getInterceptedMissiles(){
        return _interceptedMissiles;
    }

    public String getStatsToString(){
        return "Destroyed Missiles Processor: ("+getProcessingTime()+"ms)" +
                "  Intercepted Missiles: " + _interceptedMissiles.size() +
                "  Tracked Missiles: " + _trackedMissiles;
    }

    private static class MissileKiller {
        final Object api;
        final WeaponAPI weapon;
        final DamageAPI damage;
        final ShipAPI source;

        public MissileKiller(Object api, WeaponAPI weapon, DamageAPI damage, ShipAPI source) {
            this.api = api;
            this.weapon = weapon;
            this.damage = damage;
            this.source = source;
        }

        // if it was an explosion, figure out what caused the explosion
        boolean isExplosion(){
            return api instanceof DamagingExplosion;
        }

        DamagingExplosion getExplosion(){
            return (DamagingExplosion) api;
        }

        DamageAPI getDamage(){
            return damage;
        }

        public String getWeaponName(){
            return weapon == null ? Helpers.getUnknownWeaponName(damage.getType()) : weapon.getDisplayName();
        }

        ShipAPI getSource(){
            return source;
        }
    }

    public static class InterceptedMissile {
        public final ShipAPI missileSource;
        public final RawDamage missileDamage;
        public final String missileName;

        public final ShipAPI interceptionSource;
        public final String interceptionWeaponName;
        public final RawDamage interceptionDamage;

        public InterceptedMissile(ShipAPI missileSource, RawDamage missileDamage, String missileName, ShipAPI interceptionSource, String interceptionWeaponName, RawDamage interceptionDamage) {
            this.missileSource = missileSource;
            this.missileDamage = missileDamage;
            this.missileName = missileName;
            this.interceptionSource = interceptionSource;
            this.interceptionWeaponName = interceptionWeaponName;
            this.interceptionDamage = interceptionDamage;
        }
    }
}
