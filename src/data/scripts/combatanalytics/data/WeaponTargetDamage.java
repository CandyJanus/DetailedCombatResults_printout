package data.scripts.combatanalytics.data;

import com.fs.starfarer.api.Global;
import data.scripts.combatanalytics.Saveable;
import data.scripts.combatanalytics.util.Helpers;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.Map;

// A basic summary of how much damage a source ships groupName did to one target ship.
// Our most basic unit of damage, constructed post-combat by aggregating Damage objects
// as Damage objects are time based, this is battle based
public class WeaponTargetDamage implements Comparable<WeaponTargetDamage>, Saveable {
    private static final Logger log = Global.getLogger(data.scripts.combatanalytics.data.WeaponTargetDamage.class);
    private static final DecimalFormat FLOAT_SAVE_FORMAT = new DecimalFormat("#.#");

    public final String id;
    public final String combatId;
    public final Ship source;
    public final Ship target;
    public final String weapon;

    public int hits = 0;
    public double shieldDamage = 0;
    public double armorDamage = 0;
    public double hullDamage = 0;
    public double empDamage = 0;

    public double pctOfDamageDoneToTarget;
    public int killingBlow = 0;

    public WeaponTargetDamage(String[] serialized, Map<String, Map<String, Ship>> combatIdToShips){
        try {
            id = serialized[0];
            combatId = serialized[1];
            Map<String, Ship> idToShip = combatIdToShips.get(combatId);
            source = idToShip.get(serialized[2]);
            target = idToShip.get(serialized[3]);
            String rawWeapon = serialized[4];
            if(rawWeapon == null || rawWeapon.length() == 0) {
                rawWeapon = Helpers.NO_WEAPON; // band-aid fix for existing saves before checking for empty-string groupName names
            }
            weapon = rawWeapon;

            hits = Integer.parseInt(serialized[5]);
            shieldDamage = Helpers.parseFloat(serialized[6]);
            armorDamage = Helpers.parseFloat(serialized[7]);
            hullDamage = Helpers.parseFloat(serialized[8]);
            empDamage = Helpers.parseFloat(serialized[9]);
            pctOfDamageDoneToTarget = Helpers.parseFloat(serialized[10]);
            killingBlow = Integer.parseInt(serialized[11]);

            if(id == null || id.equals("")){
                throw new RuntimeException("WeaponTargetDamage combatId must be valid, combatId: '"+id+"'");
            }
            if(target == null){
                throw new RuntimeException("WeaponTargetDamage must be valid, target: '"+serialized[1]+"'");
            }
        } catch (Throwable e){
            StringBuilder line = new StringBuilder(); // String.join would be nice here
            for(String s : serialized){
                line.append("'").append(s).append("'");
                line.append('\t');
            }

            log.error("Error deserializing line (Column Count: "+serialized.length+"): " +line, e);
            throw e;
        }
    }

    public WeaponTargetDamage(String combatId, Ship source, Ship target, String weapon){
        this.id = Helpers.getSmallUuid();
        this.combatId = combatId;
        this.source = source;
        this.target = target;
        this.weapon = weapon;
    }

    public void merge(Damage d){
        hits ++;

        this.shieldDamage += d.shieldDamage;
        this.armorDamage += d.armorDamage;
        this.hullDamage += d.hullDamage;
        this.empDamage += d.empDamage;
        this.killingBlow += d.wasKillingBlow ? 1 : 0;
    }

    @Override
    public int compareTo(WeaponTargetDamage o) {
        // name, groupName
        int compare = o.target.name.compareTo(this.target.name);
        if(compare != 0){
            return compare;
        }

        compare = o.source.name.compareTo(this.source.name);
        if(compare != 0){
            return compare;
        }

        return o.weapon.compareTo(this.weapon);
    }

    @Override
    public String toString(){
        String ret = this.weapon;
        if(target != null){
            ret += " -> " + this.target.toString();
        }

        return ret;
    }

    public String serialize(){
        return Helpers.toTsv(
                id,
                combatId,
                source.id,
                target.id,
                weapon,
                hits,
                FLOAT_SAVE_FORMAT.format(shieldDamage),
                FLOAT_SAVE_FORMAT.format(armorDamage),
                FLOAT_SAVE_FORMAT.format(hullDamage),
                FLOAT_SAVE_FORMAT.format(empDamage),
                FLOAT_SAVE_FORMAT.format(pctOfDamageDoneToTarget),
                killingBlow);
    }
}
