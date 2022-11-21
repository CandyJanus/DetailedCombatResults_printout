package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;

import java.util.Objects;

class SourceTargetDamageType {
    public final ShipAPI targetShip;
    public final ShipAPI sourceShip;
    public final DamageType damageType;

    public SourceTargetDamageType(ShipAPI sourceShipApi, ShipAPI targetShip, DamageType damageType) {
        this.targetShip = targetShip;
        this.sourceShip = sourceShipApi;
        this.damageType = damageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceTargetDamageType that = (SourceTargetDamageType) o;
        return targetShip.equals(that.targetShip) &&
                sourceShip.equals(that.sourceShip) &&
                damageType == that.damageType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetShip, sourceShip, damageType);
    }

    @Override
    public String toString() {
        return sourceShip + " * " + damageType + " => " + targetShip;
    }
}
