package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static data.scripts.combatanalytics.util.Helpers.formatAsPercent;

// processes all beams active in a frame checking to see if they have done damage.
// if they have, attempts to various means to determine how much damage the beams have done
public class FrameProcessorBeam extends FrameProcessor {

    // all of the beam damages that happened this battle
    private final HashMap<SourceTargetWeapon, ReportableDamage> _beamDamages = new HashMap<>(1000);

    private final ListenerDamageInference _listenerDamageInference = new ListenerDamageInference();

    private final HashMap<String, Integer> shipIdDamageTypeToBeamCount = new HashMap<>();

    private long _resolvedCount = 0;
    private long _inferredCount = 0;
    private long _priorLookupCount = 0;

    protected FrameProcessorBeam(CombatEngineAPI engine, FrameProcessorState state, ListenerManager listenerManager) {
        super(engine, state, listenerManager);
    }


    public void internalProcessFrame(float amount){
        // gather up our beams that we'll compute damage for
        List<FrameDamage> beamsToProcess = new ArrayList<>(20);
        for (BeamAPI beam : _engine.getBeams()) {
            if (didDamageToShip(_engine, beam)) {  // missiles are handled differently
                beamsToProcess.add(new FrameDamageBeam(beam));
            }
        }

        if(beamsToProcess.size() == 0){
            return;
        }

        List<ReportableDamage> damages = processBeamDamages(beamsToProcess, amount);

        for(ReportableDamage thisFrameDamage : damages){
            // look up and see if this beam has already been doing its thing
            SourceTargetWeapon key = new SourceTargetWeapon(thisFrameDamage.sourceShip.getId(), thisFrameDamage.targetShip.getId(), thisFrameDamage.weaponName);
            ReportableDamage savedDamage = _beamDamages.get(key);
            if(savedDamage == null){
                _beamDamages.put(key, thisFrameDamage);
            } else {
                savedDamage.listenerDamage.addDamage(thisFrameDamage.listenerDamage);
                savedDamage.wasKillingBlow |= thisFrameDamage.wasKillingBlow;
            }
        }
    }

    // If this is a well formed object capable of being processed
    private boolean didDamageToShip(CombatEngineAPI engine, BeamAPI dp){
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

        // if the ship wasn't alive last frame, we aren't counting damage against it
        ShipAPI targetShip = (ShipAPI)target;
        if(!targetShip.isAlive() && !_state.aliveShipsLastFrameById.containsKey(targetShip.getId())){
            return false;
        }

        return true;
    }

    // Beam listenerDamages are only calculated every 10ms, NOT every frame.  Lots of shenanigains to deal with that
    private List<ReportableDamage> processBeamDamages(List<FrameDamage> frameDamagesToProcess, float amount){
        List<ReportableDamage> ret = new ArrayList<>(frameDamagesToProcess.size());

        // partition our projectiles that did damage by source and target
        HashMap<SourceTargetDamageType, List<FrameDamage>> frameDamagesBySourceTargetDamageType = FrameDamage.buildTargetSourceDamageTypes(frameDamagesToProcess);
        Map<SourceTargetDamageType, List<ListenerDamage>> listenerDamagesBySourceTargetDamageType = _listenerManager.getListenerDamages(true);

        for(Map.Entry<SourceTargetDamageType, List<FrameDamage>> sourceTargetDamageTypeAndDamages : frameDamagesBySourceTargetDamageType.entrySet()){
            SourceTargetDamageType std = sourceTargetDamageTypeAndDamages.getKey();
            List<FrameDamage> frameDamages = sourceTargetDamageTypeAndDamages.getValue();

            List<ListenerDamage> listenerDamages = listenerDamagesBySourceTargetDamageType.get(std);
            if(listenerDamages == null){
                listenerDamages = Collections.emptyList();
            }

            // build our inference map in case we need it
            ListenerDamageInference.InferredDamageRatio idr = _listenerDamageInference.getInferredRatio(std, listenerDamages);

            // don't even try to use the listener normally if there's more than one beam weapon.  I spent a lot of
            // time trying to get it to work but there's always at least one unsolvable edge case
            if(getDistinctBeamWeaponTypeCount(std.sourceShip, std.damageType) == 1 && listenerDamages.size() > 0) {
                // assign damage via what's in the listener, do it as a set, rather than one at a time
                List<ResolvedDamage> resolvedDamages = resolveDamageFromListener(std.sourceShip, std.targetShip, listenerDamages, frameDamages);
                _listenerDamageInference.updateLastResolvedDamages(resolvedDamages);

                for(ResolvedDamage rd : resolvedDamages){
                    // beams are always treated as having damage based on a frame time of .1, now rescale it to the proper value
                    ListenerDamage rescaled = rd.listenerDamage.rescaleBeamDamage(amount);
                    ret.add(new ReportableDamage(rd.frameDamage, rescaled, wasKillingBlow(rescaled, rd.frameDamage.getWeaponName())));
                    _resolvedCount++;
                }
            }

            for (FrameDamage fd : frameDamages) {
                // no resolved damage this frame, lets see if we resolved it in a prior frame, if so, use that
                ListenerDamage ld =_listenerDamageInference.getPriorFrameListenerDamage(fd.getSource(), fd.getTarget(), fd.getWeaponName());

                // no prior resolution, infer damage based on what other weapons have been doing recently
                if(ld == null){
                    ld = new ListenerDamage(std.sourceShip, std.targetShip, fd.getRawDamage(), idr, true);
                    _inferredCount++;
                } else {
                    _priorLookupCount++;
                }

                // beams are always treated as having damage based on a frame time of .1, now rescale it to the proper value
                ld = ld.rescaleBeamDamage(amount);
                ret.add(new ReportableDamage(fd, ld, wasKillingBlow(ld, fd.getWeaponName())));
            }

            //todo track used inference age?
        }

        return ret;
    }

    public List<ResolvedDamage> resolveDamageFromListener(ShipAPI source, ShipAPI target, List<ListenerDamage> listenerDamages, List<FrameDamage> frameDamages){
        List<ResolvedDamage> ret = new ArrayList<>();
        if(listenerDamages == null || listenerDamages.size() == 0 || frameDamages == null || frameDamages.size() == 0){
            return ret;
        }

        ListenerDamage ld = listenerDamages.get(0);
        for (FrameDamage damage : frameDamages) {
            ret.add(new ResolvedDamage(target, source, damage, ld));
        }

        listenerDamages.clear();
        frameDamages.clear();

        return ret;
    }

    private int getDistinctBeamWeaponTypeCount(ShipAPI ship, DamageType damageType){
        String key = ship.getId() + damageType;
        Integer count = shipIdDamageTypeToBeamCount.get(key);
        if(count != null){
            return count;
        }

        HashSet<Float> uniqueDamages = new HashSet<>();
        for(WeaponAPI weapon : ship.getAllWeapons()){
            if(weapon.isBeam() && weapon.getDamageType() == damageType){
                uniqueDamages.add(weapon.getDamage().getDamage() / 10);
            }
        }

        shipIdDamageTypeToBeamCount.put(key, uniqueDamages.size());

        return uniqueDamages.size();
    }

    public String getStatsToString(){
        double total = _resolvedCount + _inferredCount + _priorLookupCount;

        if(total == 0){
            return "";
        }

        return "Beam Processor ("+getProcessingTime()+"ms): " +
                "Assigned Damages: " + _resolvedCount + " ("+formatAsPercent(_resolvedCount/total)+")" +
                "  Inferred Damages: " + _inferredCount + " ("+formatAsPercent(_inferredCount/total)+")" +
                "  Prior Damages " + _priorLookupCount + " ("+formatAsPercent(_priorLookupCount/total)+")";
    }

    public String toString(){
        return getStatsToString();
    }

    public Collection<ReportableDamage> getCombatDamages() {
        return _beamDamages.values();
    }

    // KEY to lookup damages that we've done, group them all together because we can't create one per frame
    // that would blow out memory
    private static class SourceTargetWeapon {
        private final String sourceShipApiId;
        private final String targetShipApiId;
        private final String weaponName;

        public SourceTargetWeapon(String sourceShipApiId, String targetShipApiId, String weaponName) {
            this.sourceShipApiId = sourceShipApiId;
            this.targetShipApiId = targetShipApiId;
            this.weaponName = weaponName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SourceTargetWeapon that = (SourceTargetWeapon) o;
            return sourceShipApiId.equals(that.sourceShipApiId) &&
                    targetShipApiId.equals(that.targetShipApiId) &&
                    weaponName.equals(that.weaponName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(sourceShipApiId, targetShipApiId, weaponName);
        }
    }
}
