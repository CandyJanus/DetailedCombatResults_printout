package data.scripts.combatanalytics.data;

import data.scripts.combatanalytics.util.Helpers;

import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT;

/*
Our raw damage object, used in computing other saved objects.  Itself is only saved if SaveDetailedCombatData is TRUE
 */
public class Damage {
    // Members are immutable as this is a fact object
    public final float time;

    public final String weaponName;

    public final float shieldDamage;
    public final float empDamage;
    public final float armorDamage;
    public final float hullDamage;

    public final Ship firingShip;
    public final Ship targetShip;

    // we don't track remaining armor as it's not terribly meaningful as armor is location specific
    public float targetRemainingHull;
    public float targetRemainingFlux;

    public boolean wasKillingBlow;

    public final String combatId;

    public Damage(String combatId, float time, String weaponName, float shieldDamage, float armorDamage, float hullDamage, float empDamage, boolean wasKillingBlow, Ship firingShip, Ship targetShip, float targetRemainingHull, float targetRemainingFlux) {
        this.combatId = combatId;
        this.time = time;
        // Check if groupName is ""; can happen with some mod ship shipsystems
        this.weaponName = weaponName == null || weaponName.isEmpty() ? Helpers.NO_WEAPON : weaponName; //todo intern?
        this.shieldDamage = shieldDamage;
        this.empDamage = empDamage;
        this.armorDamage = armorDamage;
        this.hullDamage = hullDamage;
        this.wasKillingBlow = wasKillingBlow;
        this.firingShip = firingShip;
        this.targetShip = targetShip;
        this.targetRemainingHull = targetRemainingHull;
        this.targetRemainingFlux = targetRemainingFlux;
    }

    public String toString(){
        return String.format("%1$.2fs %2$s-->%3$s-->%4$s  %5$s shield dmg   %6$s armor dmg   %7$s hull dmg   %8$s flux dmg   %9$shp & %10$s flux remain",
                time, firingShip, weaponName, targetShip,
                INT_FORMAT.format(shieldDamage), INT_FORMAT.format(armorDamage), INT_FORMAT.format(hullDamage), INT_FORMAT.format(empDamage), targetRemainingHull, targetRemainingFlux);

        //"12.3s ISS Ship(Player)->AutoCannon->OtherShip(Computer)  400 dmg to shieldHit  900hp remain"
    }
}
