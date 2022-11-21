package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

//Infer the damage that we're doing based on how we've been doing it the most recent frame
public class ListenerDamageInference {
    private final HashMap<SourceTargetDamageType, InferredDamageRatio> _sdtToDamage = new HashMap<>();

    private final HashMap<SourceTargetWeapon, ListenerDamage> _stwToLastDamage = new HashMap<>();

    private static final InferredDamageRatio _missingRatio = new InferredDamageRatio();
    static {
        _missingRatio.armor = .2;
    }

    public InferredDamageRatio getInferredRatio(SourceTargetDamageType std, List<ListenerDamage> listenerDamages){
        if(listenerDamages == null || listenerDamages.size() == 0){
            InferredDamageRatio ret = _sdtToDamage.get(std);
            if(ret == null){
                ret = _missingRatio;
            }

            return ret;
        }

        InferredDamageRatio idr = new InferredDamageRatio();
        for (ListenerDamage ld : listenerDamages){
            idr.shield += ld.shield;
            idr.armor += ld.armor;
            idr.hull += ld.hull;
        }

        idr.normalize();
        _sdtToDamage.put(std, idr);

        return idr;
    }

    public void updateLastResolvedDamages(List<ResolvedDamage> resolvedDamages){
        for(ResolvedDamage rd : resolvedDamages){
            _stwToLastDamage.put(new SourceTargetWeapon(rd.sourceShip, rd.targetShip, rd.frameDamage.getWeaponName()), rd.listenerDamage);
        }
    }

    public ListenerDamage getPriorFrameListenerDamage(ShipAPI source, ShipAPI target, String weaponName){
        return _stwToLastDamage.get(new SourceTargetWeapon(source, target, weaponName));
    }

    static class InferredDamageRatio {
        public double shield;
        public double armor;
        public double hull;

        // normalize our values between 0 -> 1
        public void normalize(){
            double sum = shield + armor + hull;

            shield = shield / sum;
            armor = armor / sum;
            hull = hull / sum;
        }

        @Override
        public String toString() {
            return "IDR" +
                    " shield=" + shield +
                    ", armor=" + armor +
                    ", hull=" + hull;
        }
    }

    static class SourceTargetWeapon {
        public final ShipAPI targetShip;
        public final ShipAPI sourceShip;
        public final String weapon;

        public SourceTargetWeapon(ShipAPI sourceShipApi, ShipAPI targetShip, String weapon) {
            this.targetShip = targetShip;
            this.sourceShip = sourceShipApi;
            this.weapon = weapon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SourceTargetWeapon that = (SourceTargetWeapon) o;
            return targetShip.equals(that.targetShip) &&
                    sourceShip.equals(that.sourceShip) &&
                    weapon.equals(that.weapon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetShip, sourceShip, weapon);
        }

        @Override
        public String toString() {
            return "STW " + sourceShip + " * " + weapon + " => " + targetShip;
        }
    }
}
