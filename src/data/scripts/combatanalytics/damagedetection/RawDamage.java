package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.DamageType;

// damage as defined by the weapon, unmodified by skills or other factors
class RawDamage implements Comparable<RawDamage>  {
    public final float damage;
    public final float emp;
    public final DamageType type;

    public RawDamage(float damage, float emp, DamageType type) {
        this.damage = damage;
        this.emp = emp;
        this.type = type;
    }

    @Override
    public String toString() {
        return damage + " " + type.getDisplayName();
    }

    //todo test
    @Override
    public int compareTo(RawDamage o) {
        if(this.damage != o.damage){
            return Float.compare(this.damage, o.damage);
        }

        if(this.emp != o.emp){
            return Float.compare(this.emp, o.emp);
        }

        return 0;
    }
}
