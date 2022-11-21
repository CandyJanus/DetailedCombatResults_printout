package data.scripts.combatanalytics.data;

import data.scripts.combatanalytics.Exportable;
import data.scripts.combatanalytics.util.Helpers;

import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT;
import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT_NO_GROUP_FORMAT;

/**
 * Used in the export of the Detailed/Raw data.  Grouped by second.
 */
public class DamageGroupedByTime implements Comparable<DamageGroupedByTime>, Exportable {
    // key values
    public final int time;
    public final String weaponGuid;
    public final Ship firingShip;
    public final Ship targetShip;
    public final String combatResultId;

    // aggregated values
    public float shieldDamage;
    public float empDamage;
    public float armorDamage;
    public float hullDamage;
    public int hits;
    // we don't track remaining armor as it's not terribly meaningful as armor is location specific
    public float targetRemainingHull;
    public float targetRemainingFlux;

    public boolean wasKillingBlow;


    public DamageGroupedByTime(float time, String weaponGuid, Ship firingShip, Ship targetShip, String combatResultId) {
        this.time = (int) time;
        this.weaponGuid = weaponGuid;
        this.firingShip = firingShip;
        this.targetShip = targetShip;
        this.combatResultId = combatResultId;
    }

    public void addDamage(Damage damage){
        shieldDamage += damage.shieldDamage;
        empDamage += damage.empDamage;
        armorDamage += damage.armorDamage;
        hullDamage += damage.hullDamage;
        hits++;

        // set to last seen
        targetRemainingHull = damage.targetRemainingHull;
        targetRemainingFlux = damage.targetRemainingFlux;

        // set to if any
        wasKillingBlow |= damage.wasKillingBlow;
    }


    public String toString(){
        return String.format("%1$s %2$s-->%3$s-->%4$s  %11$s hits  %5$s shield dmg   %6$s armor dmg   %7$s hull dmg   %8$s flux dmg   %9$shp & %10$s flux remain",
                INT_FORMAT.format(armorDamage), firingShip, weaponGuid, targetShip,
                INT_FORMAT.format(shieldDamage), INT_FORMAT.format(armorDamage), INT_FORMAT.format(hullDamage), INT_FORMAT.format(empDamage), targetRemainingHull, targetRemainingFlux, INT_FORMAT.format(hits));

        //"12.3s ISS Ship(Player)->AutoCannon->OtherShip(Computer)  400 dmg to shieldHit  900hp remain"
    }

    //todo consider not expoting combatId, it could be inferred from the export path structure
    public String toTsv(){
        return Helpers.toTsv(
                INT_FORMAT_NO_GROUP_FORMAT.format(time),
                weaponGuid,
                INT_FORMAT_NO_GROUP_FORMAT.format(hits),
                INT_FORMAT_NO_GROUP_FORMAT.format(shieldDamage),
                INT_FORMAT_NO_GROUP_FORMAT.format(armorDamage),
                INT_FORMAT_NO_GROUP_FORMAT.format(hullDamage),
                INT_FORMAT_NO_GROUP_FORMAT.format(empDamage),
                firingShip.id,
                targetShip.id,
                wasKillingBlow ? "1" : "0",
                INT_FORMAT_NO_GROUP_FORMAT.format(targetRemainingHull),
                INT_FORMAT_NO_GROUP_FORMAT.format(targetRemainingFlux),
                combatResultId
        );
    }

    public static String getTsvHeader(){
        return Helpers.toTsv("Time", "WeaponName", "Hits", "ShieldDamage", "ArmorDamage", "HullDamage", "FluxDamage", "FiringShipId", "TargetShipId", "WasKillingBlow", "TargetRemainingHull", "TargetRemainingFlux", "BattleId");
    }

    public static String getKey(float time, String weaponGuid, String firingShipId, String targetShipId){
        return   ((int)time)
                + weaponGuid
                + firingShipId
                + targetShipId;
    }


    @Override
    public int compareTo(DamageGroupedByTime o) {
        int compare = Integer.compare(this.time, o.time);

        if(compare == 0){
            compare = Integer.compare(this.firingShip.owner, o.firingShip.owner);
        }

        if(compare == 0){
            compare = this.firingShip.name.compareTo(o.firingShip.name);
        }

        if(compare == 0){
            compare = Integer.compare(this.hits, o.hits);
        }

        return compare;
    }
}
