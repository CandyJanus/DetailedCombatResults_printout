package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

class FrameDamageBeam extends FrameDamage {
    private final BeamAPI _beam;
    private final float _amount;

    private RawDamage _computed;

    public FrameDamageBeam(BeamAPI beam){
        _beam = beam;

        // beams refresh at .1 ms.  I have no idea what happens if the computer gets behind in combat and the frame rate drops.
        // But I have a 7 year old desktop that never lags so this shouldn't be a problem for most people
        _amount = .1f;
    }

    @Override
    public String getWeaponName() {
        return _beam.getWeapon().getDisplayName();
    }

    @Override
    public RawDamage getRawDamage() {
        if(_computed != null){
            return _computed;
        }

        DamageAPI dmg = _beam.getDamage();

        if(dmg == null || dmg.getBaseDamage() == 0 && dmg.getFluxComponent() == 0 && _beam.getWeapon().getDamage() != null){
            dmg = _beam.getWeapon().getDamage();
        }

        _computed = new RawDamage(dmg.getBaseDamage() * _amount, dmg.getFluxComponent() * _amount, getDamageType());
        return _computed;
    }

    @Override
    public ShipAPI getTarget() {
        return (ShipAPI)_beam.getDamageTarget();
    }

    @Override
    public ShipAPI getSource() {
        return _beam.getSource();
    }

    @Override
    public DamageType getDamageType() {
        return _beam.getWeapon().getDamageType();
    }

    @Override
    public Vector2f getLocation() {
        return _beam.getTo();
    }

    @Override
    public DamagingProjectileAPI getProjectile() {
        return null;
    }

    @Override
    public int hashCode() {
        return _beam.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FrameDamageBeam that = (FrameDamageBeam) o;

        return _beam.equals(that._beam);
    }
}
