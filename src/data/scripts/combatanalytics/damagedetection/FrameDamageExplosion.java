package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.combat.entities.DamagingExplosion;
import org.lwjgl.util.vector.Vector2f;

/**
 * Tracks damage done to a ship via an explosion. Because explosions don't know who created them there is a fair bit
 * of magic that happens before this is created to figure that out
 */
class FrameDamageExplosion extends FrameDamage{

    private final DamagingExplosion _explosion;
    private final ShipAPI _target;
    private final String _weaponName;

    private RawDamage _computed;

    public FrameDamageExplosion(DamagingExplosion explosion, ShipAPI target, String weaponName){
        _explosion = explosion;
        _target = target;
        _weaponName = weaponName;
    }

    @Override
    public String getWeaponName() {
        return _weaponName;
    }

    @Override
    public RawDamage getRawDamage() {
        if(_computed != null){
            return _computed;
        }

        DamageAPI dmg = ((DamagingProjectileAPI)_explosion).getDamage();

        // have to do this so we can properly calculate damage if this is a sub-munition.  Submunitions
        // from the Gungnir cannon for example report as doing 0 dmg unless this is set to 1 at which point it will
        // properly report 250.  If we use the base/parent munition instead of this, it will report 1500 for each unit
        // we MUST reset the multiplier since this isn't a copy and it screws with some internal damage
        // calc
        float oldMulti = dmg.getMultiplier();

        dmg.setMultiplier(1);

        // don't get weapon here!  It's probably null.
        _computed = new RawDamage(dmg.getDamage(), dmg.getFluxComponent(), dmg.getType());

        dmg.setMultiplier(oldMulti); // reset, this isn't a copy

        return _computed;
    }

    @Override
    public ShipAPI getTarget() {
        return _target;
    }

    @Override
    public ShipAPI getSource() {
        return _explosion.getSource();
    }

    @Override
    public DamageType getDamageType() {
        return ((DamagingProjectileAPI)_explosion).getDamage().getType();
    }

    @Override
    public Vector2f getLocation() {
        return _explosion.getLocation();
    }

    @Override
    public int hashCode() {
        return _explosion.hashCode() + _target.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrameDamageExplosion that = (FrameDamageExplosion) o;

        return _explosion.equals(that._explosion) && _target.equals(that._target);
    }

    @Override
    public DamagingProjectileAPI getProjectile(){
        return _explosion;
    }
}
