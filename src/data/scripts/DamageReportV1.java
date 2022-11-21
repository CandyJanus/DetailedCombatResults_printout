package data.scripts;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;

/**
 * Contains the {@code float} armor damage and {@code float} hull damage a
 * {@link DamagingProjectileAPI}, {@link MissileAPI}, or {@link BeamAPI} has inflicted.
 * <p></p>
 * Create a DamageReport after calculating the armor and hull damage and pass it to
 * DamageReportManagerV1.getDamageReportManager().addDamageReport() for DetailedCombatResults to record and consume.
 */
public class DamageReportV1 {

    public static Object[] serialize(DamageReportV1 report){
        return new Object[]{
                report.armorDamage,
                report.hullDamage,
                report.empDamage,
                report.shieldDamage,
                report.damageType,
                report.source,
                report.target,
                report.weaponName,
        };
    }

    public static DamageReportV1 deserialize(Object[] raw){
        return new DamageReportV1(
                (float)raw[0],
                (float)raw[1],
                (float)raw[2],
                (float)raw[3],
                (DamageType)raw[4],
                (CombatEntityAPI)raw[5],
                (CombatEntityAPI)raw[6],
                (String)raw[7]
        );
    }

    // damage numbers
    private final float armorDamage;
    private final float hullDamage;
    private final float empDamage;
    private final float shieldDamage;


    private final DamageType damageType;
    private final CombatEntityAPI source;
    private final CombatEntityAPI target;
    private final String weaponName;

    public DamageReportV1(float armorDamage, float hullDamage, float empDamage, float shieldDamage, DamagingProjectileAPI projectile) {
        this.armorDamage = armorDamage;
        this.hullDamage = hullDamage;
        this.empDamage = empDamage;
        this.shieldDamage = shieldDamage;
        this.damageType = projectile.getDamageType();
        this.source = projectile.getSource();
        this.target = projectile.getDamageTarget();
        this.weaponName = projectile.getWeapon().getDisplayName();
    }

    public DamageReportV1(float armorDamage, float hullDamage, float empDamage, float shieldDamage, MissileAPI missile) {
        this.armorDamage = armorDamage;
        this.hullDamage = hullDamage;
        this.empDamage = empDamage;
        this.shieldDamage = shieldDamage;
        this.damageType = missile.getDamageType();
        this.source = missile.getSource();
        this.target = missile.getDamageTarget();
        this.weaponName = missile.getWeapon().getDisplayName();
    }

    public DamageReportV1(float armorDamage, float hullDamage, float empDamage, float shieldDamage, BeamAPI beam) {
        this.armorDamage = armorDamage;
        this.hullDamage = hullDamage;
        this.empDamage = empDamage;
        this.shieldDamage = shieldDamage;
        this.damageType = beam.getDamage().getType();
        this.source = beam.getSource();
        this.target = beam.getDamageTarget();
        this.weaponName = beam.getWeapon().getDisplayName();
    }

    public DamageReportV1(float armorDamage, float hullDamage, float empDamage, float shieldDamage, DamageType damageType, CombatEntityAPI source, CombatEntityAPI target, String weaponName) {
        this.armorDamage = armorDamage;
        this.hullDamage = hullDamage;
        this.empDamage = empDamage;
        this.shieldDamage = shieldDamage;
        this.damageType = damageType;
        this.source = source;
        this.target = target;
        this.weaponName = weaponName;
    }

    /**
     * Returns the {@code float} armor damage a {@link DamagingProjectileAPI},
     * {@link MissileAPI}, or {@link BeamAPI} has inflicted.
     */
    public float getArmorDamage() { return armorDamage; }

    /**
     * Returns the {@code float} hull damage a {@link DamagingProjectileAPI},
     * {@link MissileAPI}, or {@link BeamAPI} has inflicted.
     */
    public float getHullDamage() { return hullDamage; }

    /**
     * Returns the {@code float} emp damage a {@link DamagingProjectileAPI},
     * {@link MissileAPI}, or {@link BeamAPI} has inflicted.
     */
    public float getEmpDamage() {return empDamage;}

    /**
     * Returns the {@link DamagingProjectileAPI}, {@link MissileAPI}, or {@link BeamAPI}
     * that inflicted the {@code float} armor damage and {@code float} hull damage of
     * this {@link DamageReportV1}.
     */
    public CombatEntityAPI getSource() { return source; }

    public float getShieldDamage() {
        return shieldDamage;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    public CombatEntityAPI getTarget() {
        return target;
    }

    public String getWeaponName() {
        return weaponName;
    }
}
