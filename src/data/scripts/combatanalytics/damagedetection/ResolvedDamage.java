package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.ShipAPI;

// the product of resolving a frame damage to a listener damage (correlating what the UI frame state says with the listener state)
class ResolvedDamage {
    public final ShipAPI targetShip;
    public final ShipAPI sourceShip;
    public final FrameDamage frameDamage;
    public final ListenerDamage listenerDamage;

    public ResolvedDamage(ShipAPI targetShipApi, ShipAPI sourceShipApi, FrameDamage frameDamage, ListenerDamage listenerDamage) {
        this.targetShip = targetShipApi;
        this.sourceShip = sourceShipApi;
        this.frameDamage = frameDamage;
        this.listenerDamage = listenerDamage;
    }

    @Override
    public String toString() {
        return frameDamage.getWeaponName() + "="+ listenerDamage;
    }
}
