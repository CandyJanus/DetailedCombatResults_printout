package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

// Represents damage done via examining projectiles and beams that are active or were recently active to see if
// they have done damage
abstract class FrameDamage implements Comparable<FrameDamage> {
    // gather our frameDamages into a mapping of Target and Source to frameDamages. This will enable us to assign damages
    // as a set, rather than individually
    static HashMap<SourceTargetDamageType, List<FrameDamage>> buildTargetSourceDamageTypes(Collection<FrameDamage> frameDamages){
        HashMap<SourceTargetDamageType, List<FrameDamage>> ret = new HashMap<>();

        for(FrameDamage frameDamage : frameDamages){
            SourceTargetDamageType key = new SourceTargetDamageType(frameDamage.getSource(), frameDamage.getTarget(), frameDamage.getDamageType());

            List<FrameDamage> projectilesForTargetSource = ret.get(key);
            if(projectilesForTargetSource == null){
                projectilesForTargetSource = new ArrayList<>();
                ret.put(key, projectilesForTargetSource);
            }
            projectilesForTargetSource.add(frameDamage);
        }

        return ret;
    }

    public abstract String getWeaponName();

    public abstract RawDamage getRawDamage();

    public abstract ShipAPI getTarget();

    public abstract ShipAPI getSource();

    public abstract DamageType getDamageType();

    public abstract Vector2f getLocation();

    public abstract DamagingProjectileAPI getProjectile();

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public final int compareTo(FrameDamage o) {
        RawDamage bd = getRawDamage();
        return bd.compareTo(o.getRawDamage());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "  " + getSource().toString() + " * " +getWeaponName() + " => " + getTarget().toString() + "  " + getRawDamage();
    }

    public final boolean isCloserToLow(FrameDamage low, FrameDamage high){
        float lowDelta = Math.abs(low.getRawDamage().damage - getRawDamage().damage);
        float highDelta = Math.abs(high.getRawDamage().damage - getRawDamage().damage);

        return lowDelta < highDelta;
    }
}
