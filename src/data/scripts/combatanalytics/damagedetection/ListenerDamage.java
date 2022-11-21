package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import data.scripts.combatanalytics.util.Helpers;

import java.util.List;
import java.util.Objects;

import static data.scripts.combatanalytics.util.Helpers.DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY;
import static data.scripts.combatanalytics.util.Helpers.DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY_WITH_ARMOR;
import static data.scripts.combatanalytics.util.Helpers.computePctDeltaForDamage;

class ListenerDamage implements Comparable<ListenerDamage>{

    public static boolean areAllDamagesEqualEnough(List<ListenerDamage> damages){
        if(damages.size() < 2){
            return true;
        }

        ListenerDamage ld = damages.get(0);
        // if we have multiple damages a frame, we can't normally tell which belongs to our gun, but if they are all the same,
        // or pretty close to the same, then it doesn't matter
        for (int i=1; i<damages.size(); i++){
            if(!ld.areEqualsEnough(damages.get(i))){
                return false;
            }
        }

        return true;
    }

    // given a raw damage, locates with ListenerDamage is the closest and still inside tolerances
    public static int bestMatchIndex(float dmg, float empDmg, List<ListenerDamage> damages){
        if(damages.size() == 0){
            return -1;
        }

        int matchIndex = -1;
        float matchDistance = 999999999;
        ListenerDamage matchDamage = null;

        for(int i=0; i<damages.size(); i++){
            ListenerDamage damage = damages.get(i);

            // must or must not have EMP depending on what we're looking for
            // iff there isn't a shield hit
            if(damage.shield == 0 && damage.emp > 0 != empDmg > 0){
                continue;
            }

            float thisDamage = damage.getListedDamage();
            float newMatchDistance = Math.abs(thisDamage - dmg);
            if(newMatchDistance < matchDistance){
                matchDistance = newMatchDistance;
                matchDamage = damage;
                matchIndex = i;
            }
        }

        if(matchDamage == null){
            return -1;
        }

        //todo maybe extract this to a helper method?  We could get pretty into the weeds with this
        float maxDelta;
        if(matchDamage.getArmorHullRatio() > .1) { // it doesn't take much armor to really mess with expected damages
            maxDelta = DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY_WITH_ARMOR;
        } else {
            maxDelta = DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY;
        }

        if(computePctDeltaForDamage(dmg, matchDamage.getListedDamage()) < maxDelta){
            return matchIndex;
        } else {
            return -1;
        }
    }

    public final DamageType damageType;
    public float armor;
    public float hull;
    public float shield;
    public float emp;
    public final boolean isDps;

    public final ShipAPI source;
    public final ShipAPI target;

    private float _listedDamage = -1;

    public ListenerDamage(ShipAPI sourceShip, ShipAPI targetShip, ApplyDamageResultAPI result){
        damageType = result.getType();
        armor = result.getTotalDamageToArmor();
        hull = result.getDamageToHull();

        // very rarely result.getDamageToShields() is negative when getOverMaxDamageToShields is non zero
        shield = Math.abs(result.getDamageToShields()) + result.getOverMaxDamageToShields();
        emp = result.getEmpDamage();
        isDps = result.isDps();

        source = sourceShip;
        target = targetShip;
    }

    public ListenerDamage(ShipAPI sourceShip, ShipAPI targetShip, RawDamage rawDamage, ListenerDamageInference.InferredDamageRatio idr, boolean isDps){
        this.damageType = rawDamage.type;

        // precisely construct our damage
        float statsDamageScalar = Helpers.getSourceDamageScalar(sourceShip, targetShip, damageType, isDps);

        MutableShipStatsAPI targetStats = targetShip.getMutableStats();
        switch (damageType){

            case KINETIC:
                statsDamageScalar *= targetStats.getKineticDamageTakenMult().getModifiedValue();
                break;
            case HIGH_EXPLOSIVE:
                statsDamageScalar *= targetStats.getHighExplosiveDamageTakenMult().getModifiedValue();
                break;
            case FRAGMENTATION:
                statsDamageScalar *= targetStats.getFragmentationDamageTakenMult().getModifiedValue();
                break;
            case ENERGY:
                statsDamageScalar *= targetStats.getEnergyDamageTakenMult().getModifiedValue();
                break;
            case OTHER:
                break;
        }

        float raw = rawDamage.damage * statsDamageScalar;

        this.armor = (float) (idr.armor * raw * rawDamage.type.getArmorMult() * targetStats.getArmorDamageTakenMult().getModifiedValue()) / 3; // div by 3 to keep it in line with other listener stuff
        this.hull = (float) (idr.hull * raw * rawDamage.type.getHullMult() * targetStats.getHullDamageTakenMult().getModifiedValue());
        this.shield = (float) (idr.shield * raw * rawDamage.type.getShieldMult() * targetStats.getShieldDamageTakenMult().getModifiedValue());

        if(this.shield > 0 && targetShip.getShield() != null){
            this.shield *= targetShip.getShield().getFluxPerPointOfDamage();
        } else {
            // emp is only dealt on hull/armor hits
            this.emp = rawDamage.emp * statsDamageScalar;
        }

        this.isDps = isDps;

        source = sourceShip;
        target = targetShip;
    }

    public ListenerDamage(DamageType damageType, float armor, float hull, float shield, float emp, boolean isDps, ShipAPI source, ShipAPI target) {
        this.damageType = damageType;
        this.armor = armor;
        this.hull = hull;
        this.shield = shield;
        this.emp = emp;
        this.isDps = isDps;
        this.source = source;
        this.target = target;
    }

    public float getArmorHullRatio(){
        if(hull == 0 && armor > 0){
            return 1;
        } else if(armor == 0){
            return 0;
        } else {
            return armor / hull;
        }
    }

    public float getRealDamage(){
        return shield + hull + armor;
    }

    public ListenerDamage rescaleBeamDamage(float amount){
        float scalar = amount / .1f; // beams are recorded to listeners every .1 seconds, rescale that to this frame amount

        return new ListenerDamage(
                this.damageType,
                this.armor * scalar,
                this.hull * scalar,
                this.shield * scalar,
                this.emp * scalar,
                this.isDps,
                this.source,
                this.target
        );
    }

    public void addDamage(ListenerDamage damage){
        shield += damage.shield;
        armor += damage.armor;
        hull += damage.hull;
        emp += damage.emp;
    }

    @Override
    public String toString() {
        return  damageType + " " + getListedDamage() + "  " +
                source.getHullSpec().getHullName() + " -> " + target.getHullSpec().getHullName() +
                "  armor=" + armor +
                ", hull=" + hull +
                ", shield=" + shield +
                ", emp=" + emp +
                ", isDps=" + isDps;
    }

    // used to determine of two damages are close enough to be considered the same (many ship designs use just a few different types of weapons)
    // this aids in figuring out which damage belongs to which gun.  While it can lead to some imprecision, it's still way better
    // than trying to figure it out the other way.
    public boolean areEqualsEnough(ListenerDamage that) {

        // todo verify, should shield hits have tighter tolerances?
        float equalityDelta;
        if(this.getArmorHullRatio() > .1 && that.getArmorHullRatio() > .1){
            equalityDelta = DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY_WITH_ARMOR;
        } else {
            equalityDelta = DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY;
        }

                return computePctDeltaForDamage(
                        that.armor * 2 + that.hull / 2 + that.shield / 2+ that.emp,
                        this.armor * 2 + this.hull / 2 + this.shield /2 + this.emp
                ) < equalityDelta;
    }

    public int hashCode() {
        return Objects.hash(damageType, armor, hull, shield, emp, isDps, source, target);
    }

    @Override
    public int compareTo(ListenerDamage o) {
        return Float.compare(this.getRealDamage(), o.getRealDamage());
    }

    // Get our damage unmodified (as listed in game)
    public float getListedDamage(){
        if(_listedDamage != -1){
            return _listedDamage;
        }

        MutableShipStatsAPI targetStats = target.getMutableStats();

        float shield = this.shield * (1 / damageType.getShieldMult()) * (1 / targetStats.getShieldDamageTakenMult().getModifiedValue());
        float armor = 2 * this.armor * (1 / damageType.getArmorMult()) * (1 / targetStats.getArmorDamageTakenMult().getModifiedValue());
        float hull = this.hull * (1 / damageType.getHullMult()) * (1 / targetStats.getHullDamageTakenMult().getModifiedValue());

        if(shield == 0 && hull == 0){
            armor *= 1.5; // fudge factor
        }

        if(shield != 0 && target.getShield() != null) {
            shield *= (1 / target.getShield().getFluxPerPointOfDamage());
        }

        float totalAdjustedListenerDamage = shield + armor + hull;
        totalAdjustedListenerDamage *= 1 / Helpers.getSourceDamageScalar(source, target, damageType, isDps);

        switch (damageType){

            case KINETIC:
                totalAdjustedListenerDamage *= 1 / targetStats.getKineticDamageTakenMult().getModifiedValue();
                break;
            case HIGH_EXPLOSIVE:
                totalAdjustedListenerDamage *= 1 / targetStats.getHighExplosiveDamageTakenMult().getModifiedValue();
                break;
            case FRAGMENTATION:
                totalAdjustedListenerDamage *= 1 / targetStats.getFragmentationDamageTakenMult().getModifiedValue();
                break;
            case ENERGY:
                totalAdjustedListenerDamage *= 1 / targetStats.getEnergyDamageTakenMult().getModifiedValue();
                break;
            case OTHER:
                break;
        }

        _listedDamage =  totalAdjustedListenerDamage;

        return _listedDamage;
    }
}