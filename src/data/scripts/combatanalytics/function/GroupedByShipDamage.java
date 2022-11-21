package data.scripts.combatanalytics.function;

import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.data.WeaponTargetDamage;

import java.util.HashMap;
import java.util.Map;

/**
 * All the damage a ship did in a combat
 */
public class GroupedByShipDamage implements Comparable<GroupedByShipDamage> {

    public final String id;
    public final String name;
    public final Ship ship;
    public Map<String, DamageSet> weaponNameToDamage = new HashMap<>();
    public DamageSet allDamages = new DamageSet("All Damages");
    public AggregateDamage receivedSummary = new AggregateDamage("ReceivedSummary");

    public GroupedByShipDamage(Ship ship){
        this.ship = ship;
        this.id = ship.id;
        this.name = printShip(ship);
    }


    public void addReceivedDamage(Ship ship, WeaponTargetDamage wtd){
        receivedSummary.merge(wtd);
    }

    public void addDeliveredDamage(Ship ship, WeaponTargetDamage wtd){
        allDamages.merge(wtd);
    }

    @Override
    public String toString(){
        return this.ship.name;
    }

    @Override
    public int compareTo(GroupedByShipDamage o) {

        int ret = Double.compare(o.allDamages.aggregateShips().totalRealDamage(), this.allDamages.aggregateShips().totalRealDamage());

        if(ret == 0){
            ret = this.name.compareTo(o.name);
        }
        return ret;
    }

    private static String printShip(Ship s){
        if(s.hullSize == ShipAPI.HullSize.FIGHTER){
            return s.name;
        }
        else
        {
            return s.name + " - "+s.hullClass + " (" + s.getHullSizeString()+")" ;
        }
    }
}
