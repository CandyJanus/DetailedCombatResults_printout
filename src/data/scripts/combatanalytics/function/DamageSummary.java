package data.scripts.combatanalytics.function;

import data.scripts.combatanalytics.data.WeaponTargetDamage;

public class DamageSummary {
    public int hits = 0;
    public double shieldDamage = 0;
    public double armorDamage = 0;
    public double hullDamage = 0;
    public double empDamage = 0;

    //Careful using this value if it's not strictly aggregated to one ship, otherwise it will likely be meaningless
    public double pctOfDamageDoneToTarget = 0;

    public void merge(WeaponTargetDamage d){
        this.hits += d.hits;
        this.shieldDamage += d.shieldDamage;
        this.armorDamage += d.armorDamage;
        this.hullDamage += d.hullDamage;
        this.empDamage += d.empDamage;

        this.pctOfDamageDoneToTarget += d.pctOfDamageDoneToTarget;
    }

    public void merge(DamageSummary d){
        this.hits += d.hits;
        this.shieldDamage += d.shieldDamage;
        this.armorDamage += d.armorDamage;
        this.hullDamage += d.hullDamage;
        this.empDamage += d.empDamage;

        this.pctOfDamageDoneToTarget += d.pctOfDamageDoneToTarget;
    }

    public double totalRealDamage(){
        return shieldDamage + armorDamage + hullDamage;
    }

    public double totalAllDamage(){
        return shieldDamage + armorDamage + hullDamage + empDamage;
    }
}
