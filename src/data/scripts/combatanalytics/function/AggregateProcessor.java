package data.scripts.combatanalytics.function;

import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.data.CombatResult;
import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.data.WeaponTargetDamage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
Given a set of combat results will group the associated exchanged damages
 */
public class AggregateProcessor {

    public static GroupedByShipDamage[] aggregateWeaponDamageByShip(CombatResult combatResult){
        List<CombatResult> combatResults = new ArrayList<>();
        combatResults.add(combatResult);

        return aggregateWeaponDamageByShip(combatResults);
    }

    public static GroupedByShipDamage[] aggregateWeaponDamageByShip(List<CombatResult> combatResults){
        Map<Ship, GroupedByShipDamage> shipToStats = new HashMap<>();

        // go through all known damages, place damage in appropriate group, aggregate by damage dealt and taken
        for(CombatResult cr : combatResults){
            for(WeaponTargetDamage wtd : cr.weaponTargetDamages){

                // don't track FF
                if(wtd.source.owner == wtd.target.owner){
                    continue;
                }

                GroupedByShipDamage gd = getGroupedByShipDamage(wtd.source, shipToStats);

                DamageSet damagesForWeapon = gd.weaponNameToDamage.get(wtd.weapon);
                if(damagesForWeapon == null){
                    damagesForWeapon = new DamageSet(wtd.weapon);
                    gd.weaponNameToDamage.put(wtd.weapon, damagesForWeapon);
                }

                damagesForWeapon.merge(wtd);
                gd.addDeliveredDamage(wtd.target, wtd);

                // add that this group did damage to this target
                GroupedByShipDamage targetGroupedDamage = getGroupedByShipDamage(wtd.target, shipToStats);
                targetGroupedDamage.addReceivedDamage(gd.ship, wtd);
            }
        }

        //todo should some things like captain be aggregated by the MODE?  We could loop through all of the "ships" we've seen with
        // the same Id and create a new ship with values based on either LAST or MODE
        GroupedByShipDamage[] ret = shipToStats.values().toArray(new GroupedByShipDamage[]{});

         return ret;
    }

    private static GroupedByShipDamage getGroupedByShipDamage(Ship ship, Map<Ship, GroupedByShipDamage> shipToStats){
        GroupedByShipDamage shipGroupedDamage = shipToStats.get(ship);
        if(shipGroupedDamage == null){
            shipGroupedDamage = new GroupedByShipDamage(ship);
            shipToStats.put(ship, shipGroupedDamage);
        }

        return shipGroupedDamage;
    }

    // count of non fighter ships
    public static int getFleetSize(List<CombatResult> combatResults, int owner){
        int ret = 0;
        for(Ship s : getAllShips(combatResults, owner)){
            ret ++;
        }

        return ret;
    }

    // total value of deployment points used (non fighter)
    public static int getFleetDpValue(List<CombatResult> combatResults, int owner){
        int ret = 0;
        for(Ship s : getAllShips(combatResults, owner)){
            ret += s.deploymentPoints;
        }

        return ret;
    }

    // sum of combat durations
    public static float getCombatDuration(List<CombatResult> combatResults){
        double ret = 0d;
        for(CombatResult cr : combatResults){
            ret += cr.combatDurationSeconds;
        }

        return (float)ret;
    }

    public static List<Ship> getAllShips(List<CombatResult> combatResults, int owner){

        Set<Ship> seen = new HashSet<>();
        for(CombatResult cr : combatResults){
            for(Ship s : cr.allShips){
                if(s.owner == owner && s.hullSize != ShipAPI.HullSize.FIGHTER && s.hullSize != ShipAPI.HullSize.DEFAULT){
                    seen.add(s);
                }
            }
        }

        List<Ship> ret = new ArrayList<>(seen);
        Collections.sort(ret);

        return ret;
    }
}
