package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

class FrameDamageProjectile extends FrameDamage{

    private final DamagingProjectileAPI _proj;

    private RawDamage _computed;

    public FrameDamageProjectile(DamagingProjectileAPI proj){
        _proj = proj;
    }

    @Override
    public String getWeaponName() {
        return _proj.getWeapon().getDisplayName();
    }

    @Override
    public RawDamage getRawDamage() {
        if(_computed != null){
            return _computed;
        }

        DamageAPI dmg = _proj.getDamage();

        // have to do this so we can properly calculate damage if this is a sub-munition.  Submunitions
        // from the Gungnir cannon for example report as doing 0 dmg unless this is set to 1 at which point it will
        // properly report 250.  If we use the base/parent munition instead of this, it will report 1500 for each unit
        // we MUST reset the multiplier since this isn't a copy and it screws with some internal damage
        // calc
        float oldMulti = dmg.getMultiplier();

        dmg.setMultiplier(1);

        if(dmg.getDamage() == 0 && dmg.getFluxComponent() == 0 && _proj.getWeapon().getDamage() != null){
            dmg = _proj.getWeapon().getDamage();
        }

        _computed = new RawDamage(dmg.getDamage(), dmg.getFluxComponent(), dmg.getType());

        _proj.getDamage().setMultiplier(oldMulti); // reset, this isn't a copy

        return _computed;
    }

    @Override
    public ShipAPI getTarget() {
        return (ShipAPI) _proj.getDamageTarget();
    }

    @Override
    public ShipAPI getSource() {
        return _proj.getWeapon().getShip();
    }

    @Override
    public DamageType getDamageType() {
        return _proj.getWeapon().getDamageType();
    }

    @Override
    public Vector2f getLocation() {
        return _proj.getLocation();
    }

    @Override
    public int hashCode() {
        return _proj.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrameDamageProjectile that = (FrameDamageProjectile) o;

        return _proj.equals(that._proj);
    }

    @Override
    public DamagingProjectileAPI getProjectile(){
        return _proj;
    }
}
