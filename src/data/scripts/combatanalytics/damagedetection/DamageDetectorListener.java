package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import data.scripts.combatanalytics.util.Helpers;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static data.scripts.combatanalytics.util.Constants.MINE;

// Attached to each ship, tracks damage amounts done to it by other ships but NOT other ships weapons,
// so it's only sort-of useful
class DamageDetectorListener implements DamageListener {

    private static final Logger log = Global.getLogger(DamageDetectorListener.class);

    public ShipAPI ship;

    private final HashMap<String, DamagesByType> _sourceShipIdToProjectileDamages = new HashMap<>();
    private final HashMap<String, DamagesByType> _sourceShipIdToDpsDamages = new HashMap<>();
    private final List<ReportableDamage> _ballisticDamages = new ArrayList<>();

    public DamageDetectorListener(ShipAPI ship){
        this.ship = ship;
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if(source == null || result == null || result.getType() == DamageType.OTHER){ // I've never seen OTHER be anything meaningful.
            return;
        }

        if(!(target instanceof ShipAPI)){
            return;
        }
        ShipAPI targetShip = (ShipAPI) target;

        ShipAPI sourceShip;
        try {
            if (source instanceof ShipAPI) {
                sourceShip = (ShipAPI) source;
                if (sourceShip != targetShip) {
                    addDamage(sourceShip, targetShip, result);
                }

            } else if (source instanceof DamagingProjectileAPI) { // lightning gun from SWP needs this
                DamagingProjectileAPI projectile = (DamagingProjectileAPI) source;
                sourceShip = projectile.getSource();
                if (sourceShip != targetShip) {
                    ListenerDamage ld = new ListenerDamage(sourceShip, targetShip, result);
                    if (projectile.getWeapon() != null && projectile.getWeapon().getDisplayName() != null) {
                        _ballisticDamages.add(new ReportableDamage(new FrameDamageProjectile(projectile), ld, false)); // killing blow determined in FrameProcessorProjectile
                    } else if (projectile instanceof MissileAPI) {
                        MissileAPI m = (MissileAPI) projectile;
                        String name = m.isMine() ? MINE : Helpers.getUnknownWeaponName(projectile.getDamageType(), "Projectile");
                        _ballisticDamages.add(new ReportableDamage(name, ld, false)); // killing blow determined in FrameProcessorProjectile
                    } else {
                        String name = Helpers.getUnknownWeaponName(projectile.getDamageType(), "Projectile");
                        _ballisticDamages.add(new ReportableDamage(name, ld, false)); // killing blow determined in FrameProcessorProjectile
                    }
                }
            } else {
                return;
            }
        } catch (Throwable t){
            log.error("Error listening for damages", t);
        }
    }

    private void addDamage(ShipAPI sourceShip, ShipAPI targetShip, ApplyDamageResultAPI result){
        ListenerDamage damage = new ListenerDamage(sourceShip, targetShip, result);

        HashMap<String, DamagesByType> map;
        if(result.isDps()){
            map = _sourceShipIdToDpsDamages;
        } else {
            map = _sourceShipIdToProjectileDamages;
        }

        DamagesByType damages = map.get(sourceShip.getId());
        if(damages == null){
            damages = new DamagesByType(sourceShip, targetShip);
            map.put(sourceShip.getId(), damages);
        }

        damages.add(damage);
    }

    public void dispose(){
        if(ship != null) {
            ship.removeListenerOfClass(DamageDetectorListener.class);
            ship = null;
        }
    }

    public DamagesByType getDamageResultsBySource(ShipAPI sourceShip, boolean isBeam){
        if(isBeam) {
            return _sourceShipIdToDpsDamages.get(sourceShip.getId());
        } else {
            return _sourceShipIdToProjectileDamages.get(sourceShip.getId());
        }
    }

    public Map<SourceTargetDamageType, List<ListenerDamage>> getAllDamages(boolean isBeam){
        Map<SourceTargetDamageType, List<ListenerDamage>> ret = new HashMap<>();

        HashMap<String, DamagesByType> map;
        if(isBeam){
            map = _sourceShipIdToDpsDamages;
        } else {
            map = _sourceShipIdToProjectileDamages;
        }

        for(DamagesByType damagesByType : map.values()){
            if(damagesByType.kinetic.size() > 0){
                SourceTargetDamageType stdt = new SourceTargetDamageType(damagesByType.source, damagesByType.target, DamageType.KINETIC);
                ret.put(stdt, damagesByType.kinetic);
            }
            if(damagesByType.energy.size() > 0){
                SourceTargetDamageType stdt = new SourceTargetDamageType(damagesByType.source, damagesByType.target, DamageType.ENERGY);
                ret.put(stdt, damagesByType.energy);
            }
            if(damagesByType.fragmentation.size() > 0){
                SourceTargetDamageType stdt = new SourceTargetDamageType(damagesByType.source, damagesByType.target, DamageType.FRAGMENTATION);
                ret.put(stdt, damagesByType.fragmentation);
            }
            if(damagesByType.highExplosive.size() > 0){
                SourceTargetDamageType stdt = new SourceTargetDamageType(damagesByType.source, damagesByType.target, DamageType.HIGH_EXPLOSIVE);
                ret.put(stdt, damagesByType.highExplosive);
            }
        }

        return ret;
    }

    public List<ReportableDamage> getBallisticDamages() {
        return _ballisticDamages;
    }

    public void clearDamageResults(){
        _sourceShipIdToDpsDamages.clear();
        _sourceShipIdToProjectileDamages.clear();
        _ballisticDamages.clear();
    }

    @Override
    public String toString() {
        return ship.toString() + " Projectile Damage Count: " + _sourceShipIdToProjectileDamages.size() + "  Beam Damage Count: " + _sourceShipIdToDpsDamages.size();
    }

    public static class DamagesByType {
        public final ShipAPI source;
        public final ShipAPI target;

        public List<ListenerDamage> highExplosive = new ArrayList<>();
        public List<ListenerDamage> kinetic = new ArrayList<>();
        public List<ListenerDamage> energy = new ArrayList<>();
        public List<ListenerDamage> fragmentation = new ArrayList<>();

        public DamagesByType(ShipAPI source, ShipAPI target) {
            this.source = source;
            this.target = target;
        }

        public void add(ListenerDamage ld){
            switch (ld.damageType){
                case KINETIC:
                    kinetic.add(ld);
                    break;
                case HIGH_EXPLOSIVE:
                    highExplosive.add(ld);
                    break;
                case FRAGMENTATION:
                    fragmentation.add(ld);
                    break;
                case ENERGY:
                    energy.add(ld);
                    break;
                case OTHER:
                    break;
            }
        }
    }
}
