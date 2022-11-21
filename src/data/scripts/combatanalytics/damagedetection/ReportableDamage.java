package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.util.Settings;

import java.util.Objects;

class ReportableDamage {
    // Set to true if you want to see the damage of individual fighter weapons
    private static final boolean FighterWeaponBreakout = Settings.FighterWeaponBreakout;

    public final ListenerDamage listenerDamage;

    public ShipAPI targetShip;
    public ShipAPI sourceShip;
    public String weaponName;

    // So we can't log the same damage multiple times if for some reason it gets reported across multiple frames
    public int damagingEntityId; //todo special case -1

    public boolean wasKillingBlow;

    ReportableDamage(FrameDamage fd, ListenerDamage ld, boolean wasKillingBlow) {
        listenerDamage = ld;
        this.wasKillingBlow = wasKillingBlow;
        this.damagingEntityId = fd.getProjectile() != null ? System.identityHashCode(fd.getProjectile()) : -1;

        targetShip = ld.target;
        sourceShip = ld.source;

        weaponName = fd.getWeaponName();
        // while fighters might be "ships" we treat them as a weapons system for purposes of analytics
        if (sourceShip.isFighter() && !sourceShip.isDrone()) {
            weaponName = GetFighterWeaponName(sourceShip, weaponName);
            if (sourceShip.getWing() != null && sourceShip.getWing().getSourceShip() != null) { // can be null in the main screen, or maybe a crazy mod
                sourceShip = sourceShip.getWing().getSourceShip();
            }
        } else if (sourceShip.isDrone() && sourceShip.getDroneSource() != null) { // drones are mostly from mods these days
            weaponName = GetDroneWeaponName(sourceShip, weaponName);
            sourceShip = sourceShip.getDroneSource();
        }
    }

    ReportableDamage(String weaponName, ListenerDamage ld, boolean wasKillingBlow) {
        this.weaponName = weaponName;
        this.listenerDamage = ld;
        this.wasKillingBlow = wasKillingBlow;
        this.damagingEntityId = -1;

        targetShip = ld.target;
        sourceShip = ld.source;
    }

    private String GetFighterWeaponName(ShipAPI sourceShip, String weaponName){
        String ret = sourceShip.getHullSpec().getHullName() + " Fighter";
        if(FighterWeaponBreakout){
            ret += " - " + weaponName;
        }

        return ret;
    }

    private String GetDroneWeaponName(ShipAPI sourceShip, String weaponName){
        String ret = sourceShip.getHullSpec().getHullName() +" Drone";
        if(FighterWeaponBreakout){
            ret += " - " + weaponName;
        }

        return ret;
    }

    @Override
    public String toString() {
        return sourceShip.toString() + " * " +weaponName + " => " + targetShip.toString() + "  " + listenerDamage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;
        ReportableDamage that = (ReportableDamage) o;
        return damagingEntityId == that.damagingEntityId &&
                targetShip.equals(that.targetShip) &&
                sourceShip.equals(that.sourceShip) &&
                weaponName.equals(that.weaponName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetShip, sourceShip, weaponName, damagingEntityId);
    }
}
