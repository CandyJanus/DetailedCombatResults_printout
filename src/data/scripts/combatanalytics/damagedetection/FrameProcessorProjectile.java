package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.combat.entities.DamagingExplosion;
import data.scripts.combatanalytics.util.LRUMap;
import data.scripts.combatanalytics.util.VectorUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static data.scripts.combatanalytics.util.Helpers.DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY;
import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT_NO_GROUP_FORMAT;
import static data.scripts.combatanalytics.util.Helpers.computePctDeltaForDamage;
import static data.scripts.combatanalytics.util.Helpers.formatAsPercent;

public class FrameProcessorProjectile extends FrameProcessor {

    // LRU so we don't blow through memory.  Projectiles will claim to do damage for multiple frames, this is not true.
    private final LRUMap<ProjectileTargetPair, ProjectileTargetPair> _loggedProjectiles = new LRUMap<>(1000, 10000);

    private HashSet<DamagingProjectileAPI> _lastFrameProjectiles = new HashSet<>(100);

    // all of the projectile damages that happened this battle
    private final HashSet<ReportableDamage> _pointInTimeDamages = new HashSet<>(5000);

    //missiles active in the last frame
    private HashSet<MissileAPI> _seenMissiles = new HashSet<>(100);

    private final ListenerDamageInference _listenerDamageInference = new ListenerDamageInference();

    private long _assignedProjectileDamages = 0;
    private long _unassignedProjectileDamages = 0;
    private long _unassignedProjectiles = 0;

    private double _totalAssignedDamage = 0;
    private double _totalUnassignedFrameDamage = 0;
    private double _totalUnassignedListenerDamage = 0;

    protected FrameProcessorProjectile(CombatEngineAPI engine, FrameProcessorState state, ListenerManager listenerManager) {
        super(engine, state, listenerManager);
    }

    public void internalProcessFrame(float amount){
        // some ballistic damages can be fully and completely determined by our listener
        List<ReportableDamage> fullDamages = _listenerManager.getBallisticDamages();

        for(ReportableDamage fd : fullDamages){
            ProjectileTargetPair ptp = new ProjectileTargetPair(fd.damagingEntityId, fd.sourceShip, fd.targetShip);
            fd.wasKillingBlow = wasKillingBlow(fd.listenerDamage, fd.weaponName);
            _loggedProjectiles.put(ptp, ptp);
        }
        _pointInTimeDamages.addAll(fullDamages);

        List<FrameDamage> projectilesToProcess = getProjectilesToProcess();
        if(projectilesToProcess.size() == 0){
            return;
        }

        projectilesToProcess = removeUnnecessaryProjectiles(projectilesToProcess);

        List<ReportableDamage> damages = processProjectileDamages(projectilesToProcess);
        _pointInTimeDamages.addAll(damages);

        for(FrameDamage fd : projectilesToProcess){
            ProjectileTargetPair ptp = new ProjectileTargetPair(fd.getProjectile(), fd.getSource(), fd.getTarget());
            _loggedProjectiles.put(ptp, ptp);
        }
    }

    // If this is a well formed object capable of being processed
    private boolean didDamageToShip(CombatEngineAPI engine, DamagingExplosion dp, CombatEntityAPI target){
        // not all DamagingProjectileAPI have targets or weapons (explosions), so those have to be specified
        if(target == null){
            return false;
        }
        if (!(target instanceof ShipAPI)) { // only track damage done to ships
            return false;
        }

        ProjectileTargetPair key = new ProjectileTargetPair(dp, dp.getSource(), target);
        if (_loggedProjectiles.containsKey(key)) {    // if we've already tracked this projectiles damage
            return false;
        }

        // if the ship wasn't alive last frame, we aren't counting damage against it
        ShipAPI targetShip = (ShipAPI)target;
        if(!targetShip.isAlive() && !_state.aliveShipsLastFrameById.containsKey(targetShip.getId())){
            return false;
        }

        return true;
    }

    private boolean didDamageToShip(CombatEngineAPI engine, DamagingProjectileAPI dp){
        if (dp.getWeapon() == null || dp.getWeapon().getShip() == null || dp.getDamage() == null || dp.getWeapon().getDisplayName() == null) { // mods do some weird stuff, defend against it
            return false;
        }

        CombatEntityAPI target = dp.getDamageTarget();
        if(target == null){
            return false;
        }
        if (!(target instanceof ShipAPI)) { // only track damage done to ships
            return false;
        }

        ProjectileTargetPair key = new ProjectileTargetPair(dp, dp.getSource(), target);
        if (_loggedProjectiles.containsKey(key)) {    // if we've already tracked this projectiles damage
            return false;
        }

        // if the ship wasn't alive last frame, we aren't counting damage against it
        ShipAPI targetShip = (ShipAPI)target;
        if(!targetShip.isAlive() && !_state.aliveShipsLastFrameById.containsKey(targetShip.getId())){
            return false;
        }

        return true;
    }

    private List<FrameDamage> getProjectilesToProcess(){
        // missiles currently alive in this frame.  The only way to tell when a missile has done damage is when it
        // doesn't exist this frame but did the prior AND that it claims to have done damage
        HashSet<MissileAPI> activeMissiles = new HashSet<>(100);

        // gather up our projectiles that we'll compute damage for
        List<FrameDamage> projectilesToProcess = new ArrayList<>();

        // projectiles we've seen this frame that aren't missiles or haven't done damage.  They *might* be
        // special projectiles that don't do damage like normal stuff (plasma & flak)
        HashSet<DamagingProjectileAPI> thisFrameProjectiles = new HashSet<>(100);
        for (DamagingProjectileAPI proj : _engine.getProjectiles()) {
            if (proj instanceof MissileAPI) {  // missiles are handled differently
                activeMissiles.add((MissileAPI) proj);
            } else if (proj instanceof DamagingExplosion) {
                DamagingExplosion explosion = (DamagingExplosion) proj;
                for(CombatEntityAPI damagedByExplosion : explosion.getDamagedAlready()){
                    if(damagedByExplosion == explosion.getSource()){
                        continue; // for some reason the source ends up in the damaged list when it's just not true.
                    }

                    if(didDamageToShip(_engine, explosion, damagedByExplosion)) {
                        projectilesToProcess.add(new FrameDamageExplosion(explosion, (ShipAPI)damagedByExplosion, _state.getExplosionCause(explosion)));
                    }
                }
            } else  {
                // projectiles that do damage normally
                if(didDamageToShip(_engine, proj) && proj.didDamage()){
                    projectilesToProcess.add(new FrameDamageProjectile(proj));
                    _lastFrameProjectiles.remove(proj); // it did damage, remove it so we can't double count
                } else {
                    thisFrameProjectiles.add(proj);
                }
            }
        }

        // find our projectiles that were removed and don't claim to have done damage BUT
        // have an assigned target, which only happens when you damage a ship
        _lastFrameProjectiles.removeAll(thisFrameProjectiles);
        for(DamagingProjectileAPI proj : _lastFrameProjectiles){
            if(didDamageToShip(_engine, proj)) {
                projectilesToProcess.add(new FrameDamageProjectile(proj));
            }
        }
        _lastFrameProjectiles = thisFrameProjectiles;

        // determine which missiles are dead, see if they did damage. http://fractalsoftworks.com/forum/index.php?topic=11076.0
        _seenMissiles.removeAll(activeMissiles);
        for (MissileAPI missile : _seenMissiles) {
            if(didDamageToShip(_engine, missile) && missile.didDamage()){
                projectilesToProcess.add(new FrameDamageProjectile(missile));
            }
        }
        _seenMissiles = activeMissiles;

        return projectilesToProcess;
    }

    private List<FrameDamage> removeUnnecessaryProjectiles(List<FrameDamage> projectiles){
        List<FrameDamage> ret = new ArrayList<>(projectiles.size());
        // explosions and missiles when they appear to damage in the same frame, the explosion is probably caused
        // by the missile IFF they have the same name and are about in the same location.  Only keep the explosion
        // instead of the missile since the explosion is what damages multiple targets

        List<FrameDamage> explosiveDamages = new ArrayList<>();
        for(FrameDamage fd : projectiles){
            if(fd instanceof FrameDamageExplosion){
                explosiveDamages.add(fd);
            }
        }

        for(FrameDamage fd : projectiles){
            if(fd instanceof FrameDamageExplosion){
                ret.add(fd);
                continue;
            }

            if(!sharesSameLocationAndName(fd, explosiveDamages)) {
                ret.add(fd);
            }
        }

        return ret;
    }

    private boolean sharesSameLocationAndName(FrameDamage fd, List<FrameDamage> others){
        for(FrameDamage other : others){
            double thisDistance = VectorUtil.distance(fd.getLocation(), other.getLocation());
            if(thisDistance < 25 && fd.getWeaponName().equals(other.getWeaponName())){
                return true;
            }
        }

        return false;
    }

    private List<ReportableDamage> processProjectileDamages(List<FrameDamage> frameDamagesToProcess){
        List<ReportableDamage> ret = new ArrayList<>(frameDamagesToProcess.size());

        // partition our projectiles that did damage by source and target
        HashMap<SourceTargetDamageType, List<FrameDamage>> frameDamagesBySourceTargetDamageType = FrameDamage.buildTargetSourceDamageTypes(frameDamagesToProcess);
        Map<SourceTargetDamageType, List<ListenerDamage>> listenerDamagesBySourceTargetDamageType = _listenerManager.getListenerDamages(false);

        for(Map.Entry<SourceTargetDamageType, List<FrameDamage>> sourceTargetDamageTypeAndDamages : frameDamagesBySourceTargetDamageType.entrySet()){
            SourceTargetDamageType std = sourceTargetDamageTypeAndDamages.getKey();
            List<FrameDamage> frameDamages = sourceTargetDamageTypeAndDamages.getValue();

            List<ListenerDamage> listenerDamages = listenerDamagesBySourceTargetDamageType.get(std);
            if(listenerDamages == null){
                listenerDamages = Collections.emptyList();
            }

            // build our inference map in case we need it (we almost never do for projectiles)
            ListenerDamageInference.InferredDamageRatio idr = _listenerDamageInference.getInferredRatio(std, listenerDamages);

            // assign damage via what's in the listener, do it as a set, rather than one at a time
            List<ResolvedDamage> resolvedDamages = resolveDamageFromListener(std.sourceShip, std.targetShip, listenerDamages, frameDamages);

            // ships exploding, these unused damages don't count.  Don't add them to our "unassigned"
            if(std.damageType == DamageType.HIGH_EXPLOSIVE && _state.isShipExploding(std.targetShip)){
                listenerDamages.clear();
            }

            if(resolvedDamages.size() > 0 && frameDamages.size() > 0 && listenerDamages.size() > 0){
                resolvedDamages.addAll(resolveDamageFromListener(std.sourceShip, std.targetShip, listenerDamages, frameDamages));
            }

            for(ResolvedDamage rd : resolvedDamages){
                ret.add(new ReportableDamage(rd.frameDamage, rd.listenerDamage, wasKillingBlow(rd.listenerDamage, rd.frameDamage.getWeaponName())));
            }

            for (FrameDamage fd : frameDamages) {
                ListenerDamage ld = new ListenerDamage(std.sourceShip, std.targetShip, fd.getRawDamage(), idr, false);
                ret.add(new ReportableDamage(fd, ld, wasKillingBlow(ld, fd.getWeaponName())));
            }

            _assignedProjectileDamages += resolvedDamages.size();
            for(ResolvedDamage rd : resolvedDamages){
                _totalAssignedDamage += rd.listenerDamage.getListedDamage();
            }

            _unassignedProjectileDamages += listenerDamages.size();
            for(ListenerDamage ld : listenerDamages){
                _totalUnassignedListenerDamage += ld.getListedDamage();
            }

            _unassignedProjectiles += frameDamages.size();
            for(FrameDamage fd : frameDamages){
                _totalUnassignedFrameDamage += fd.getRawDamage().damage;
            }
        }

        //TODO!  need a way to prevent the reporting of the same damage multiple times over multiple frames (that can be far apart).  Maybe system.identityHashCode ????

        return ret;
    }

    // try various techniques to match a set of things that did damage, to a set of recorded listenerDamages
    public List<ResolvedDamage> resolveDamageFromListener(ShipAPI source, ShipAPI target, List<ListenerDamage> listenerDamages, List<FrameDamage> frameDamages){
        List<ResolvedDamage> ret = new ArrayList<>();
        if(listenerDamages == null || listenerDamages.size() == 0 || frameDamages == null || frameDamages.size() == 0){
            return ret;
        }

        // optimization as this is 80% of all cases
        if(frameDamages.size() == 1 && listenerDamages.size() == 1){
            ret.add(new ResolvedDamage(target, source, frameDamages.get(0), listenerDamages.get(0)));

            listenerDamages.clear();
            frameDamages.clear();

            return ret;
        }

        // both equal length, sort and assign
        if (frameDamages.size() == listenerDamages.size()) {
            Collections.sort(frameDamages);
            Collections.sort(listenerDamages);

            for(int i=0; i<frameDamages.size(); i++){
                ret.add(new ResolvedDamage(target, source, frameDamages.get(i), listenerDamages.get(i)));
            }

            listenerDamages.clear();
            frameDamages.clear();

            return ret;
        }

        HashSet<String> weapons = new HashSet<>();
        for (FrameDamage frameDamage : frameDamages) {
            weapons.add(frameDamage.getWeaponName());
        }

        // all of the listenerDamages are basically the same, and there's only one type of weapon involved, just assign them all.
        if(weapons.size() == 1 && ListenerDamage.areAllDamagesEqualEnough(listenerDamages) && listenerDamages.size() < 5){
            // listeners will do some weird stuff with not reporting all damages that were done, ESPECIALLY with beams of the same type. Like 99% of the time.
            // so just grab the first LD and assign it to every FD

            int size = Math.max(frameDamages.size(), listenerDamages.size());
            for(int i=0; i<size; i++) {
                ret.add(new ResolvedDamage(target, source,
                        frameDamages.get(Math.min(i, frameDamages.size()-1)),
                        listenerDamages.get(Math.min(i, listenerDamages.size()-1))
                ));
            }

            listenerDamages.clear();
            frameDamages.clear();

            return ret;
        }

        // only two types of weapons, sort and assign based on which you're closer to
        if(weapons.size() == 2 && frameDamages.size() >= listenerDamages.size()){
            Collections.sort(frameDamages);
            Collections.sort(listenerDamages);

            ListenerDamage lowLd = listenerDamages.get(0);
            ListenerDamage highLd = listenerDamages.get(listenerDamages.size() - 1);
            FrameDamage lowFd = frameDamages.get(0);
            FrameDamage highFd = frameDamages.get(frameDamages.size() - 1);

            // there's only one listener damage for two different weapons.  This is only OK if they do more or less the same damage.
            // of if there are more than one listener damage to choose from
            if(listenerDamages.size() > 1 || computePctDeltaForDamage(lowFd.getRawDamage().damage, highFd.getRawDamage().damage) < DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY) {
                ret.add(new ResolvedDamage(target, source, lowFd, lowLd));
                ret.add(new ResolvedDamage(target, source, highFd, highLd));

                for (int i = 0; i < frameDamages.size() - 1; i++) {
                    FrameDamage fd = frameDamages.get(i);
                    ret.add(new ResolvedDamage(target, source, fd, fd.isCloserToLow(lowFd, highFd) ? lowLd : highLd));
                }

                listenerDamages.clear();
                frameDamages.clear();

                return ret;
            }
        }

        // data is a bit of a mess, see if any of our frame damages is about as much damage as we would expect in our listener damages.
        if(frameDamages.size() > 0 && listenerDamages.size() > 0) {
            // find which damages most closely match
            for (FrameDamage fd : new ArrayList<>(frameDamages)) {
                int matchIndex = ListenerDamage.bestMatchIndex(fd.getRawDamage().damage, fd.getRawDamage().emp, listenerDamages);
                if (matchIndex > -1) {
                    ret.add(new ResolvedDamage(target, source, fd, listenerDamages.get(matchIndex)));
                    listenerDamages.remove(matchIndex);
                    frameDamages.remove(fd);
                }
            }
        }

        // it's hard to guess
        return ret;
    }

    public String getStatsToString(){
        return
                "Projectile Processing ("+getProcessingTime()+"ms):" +
                " Accuracy: " + formatAsPercent(accuracy(_assignedProjectileDamages, _unassignedProjectileDamages, _unassignedProjectiles)) +
                "  Resolved Damages: " + _assignedProjectileDamages + " ("+INT_FORMAT_NO_GROUP_FORMAT.format(_totalAssignedDamage) + ")" +
                "  Unassigned ListenerDamages: " + _unassignedProjectileDamages + " ("+INT_FORMAT_NO_GROUP_FORMAT.format(_totalUnassignedListenerDamage) + ")" +
                "  Unassigned FrameDamages: " + _unassignedProjectiles + " ("+INT_FORMAT_NO_GROUP_FORMAT.format(_totalUnassignedFrameDamage) + ")";
    }

    private static float accuracy(double assignedDamages, double unassignedDamages, double unassignedFrameDamages){
        double total = assignedDamages + unassignedDamages + unassignedFrameDamages;
        if(total == 0){
            return 1f;
        }
        return (float) (assignedDamages / total);
    }

    public Collection<ReportableDamage> getCombatDamages() {
        return _pointInTimeDamages;
    }

    public String toString(){
        return getStatsToString();
    }

    private static class ProjectileTargetPair {
        final int damagingEntityId;
        final CombatEntityAPI source;
        final CombatEntityAPI target;

        public ProjectileTargetPair(int damagingEntityId, CombatEntityAPI source, CombatEntityAPI target) {
            this.damagingEntityId = damagingEntityId;
            this.source = source;
            this.target = target;
        }

        public ProjectileTargetPair(DamagingProjectileAPI projectileAPI, CombatEntityAPI source, CombatEntityAPI target) {
            this.damagingEntityId = System.identityHashCode(projectileAPI);
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProjectileTargetPair that = (ProjectileTargetPair) o;
            return damagingEntityId == that.damagingEntityId &&
                    source.equals(that.source) &&
                    target.equals(that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(damagingEntityId, source, target);
        }
    }
}
