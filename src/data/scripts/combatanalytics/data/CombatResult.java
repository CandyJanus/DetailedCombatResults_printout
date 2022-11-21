package data.scripts.combatanalytics.data;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.Exportable;
import data.scripts.combatanalytics.Saveable;
import data.scripts.combatanalytics.util.Helpers;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT;
import static data.scripts.combatanalytics.util.Helpers.ISO_DATE_TIME;

public class CombatResult implements Comparable<CombatResult>, Saveable, Exportable {
    private static final Logger log = Global.getLogger(data.scripts.combatanalytics.data.CombatResult.class);

    public final String combatId;
    public final String faction;
    public final String fleetName;
    public final long engagementEndTime; //UTC

    public final float combatDurationSeconds;

    public final WeaponTargetDamage[] weaponTargetDamages;

    public final CombatGoal enemyFleetGoal;

    public final Ship[] allShips;

    /**
     * Deserializer constructor
     */
    public CombatResult(String[] serialized, Map<String, List<WeaponTargetDamage>> combatIdToWTDs, Map<String, Map<String, Ship>> combatIdToShips){
        try {
            combatId = serialized[0];
            faction = serialized[1];
            fleetName = serialized[2];
            engagementEndTime = Long.parseLong(serialized[3]);
            combatDurationSeconds = Helpers.parseFloat(serialized[4]);

            // there might not be any damages if no damage was actually exchanged (super rare)
            List<WeaponTargetDamage> wtdList = combatIdToWTDs.get(combatId);
            if(wtdList == null){
                weaponTargetDamages = new WeaponTargetDamage[0];
            } else {
                weaponTargetDamages = wtdList.toArray(new WeaponTargetDamage[]{});
            }

            enemyFleetGoal = CombatGoal.valueOf(serialized[5]);
            allShips = combatIdToShips.get(combatId).values().toArray(new Ship[]{});
            Arrays.sort(allShips);
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

    public CombatResult(String combatId, String faction, String fleetName, long engagementEndTime,
                        Damage[] damages, float combatDurationSeconds, CombatGoal enemyFleetGoal, Ship[] allShips) {
        this.combatId = combatId;
        this.faction = faction;
        this.fleetName = fleetName;
        this.engagementEndTime = engagementEndTime;
        this.combatDurationSeconds = combatDurationSeconds;

        this.enemyFleetGoal = enemyFleetGoal;

        this.allShips = allShips;

        weaponTargetDamages = buildWeaponTargetDamages(damages, combatId);
        log.debug("Computing combat results from " + damages.length + " Damages  =>  " + weaponTargetDamages.length + " WeaponTargetDamages");
    }

    public int getEnemyFleetSize(){
        int ret = 0;
        for(Ship s : allShips){
            if(s.owner == 1 && s.hullSize != ShipAPI.HullSize.FIGHTER && s.hullSize != ShipAPI.HullSize.DEFAULT){
                ret++;
            }
        }

        return ret;
    }

    public int getEnemyDeploymentPoints(){
        int ret = 0;
        for(Ship s : allShips){
            if(s.owner == 1 && s.hullSize != ShipAPI.HullSize.FIGHTER && s.hullSize != ShipAPI.HullSize.DEFAULT){
                ret += s.deploymentPoints;
            }
        }

        return ret;
    }

    public String toString(){
        float totalDamage = 0;
        for(WeaponTargetDamage wtd : weaponTargetDamages){
            totalDamage += (wtd.armorDamage + wtd.hullDamage + wtd.shieldDamage);
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(engagementEndTime);

        return String.format("%1$s %2$s (%3$s total ships involved) on %4$s  %5$s total damage exchanged",
                faction, fleetName, this.allShips.length, ISO_DATE_TIME.format(cal.getTime()), INT_FORMAT.format(totalDamage));
    }

    // aggregate the currentBattleDamage data removing the time component
    private static WeaponTargetDamage[] buildWeaponTargetDamages(Damage[] damages, String combatId){
        // map of firing ship to groupName damage
        Map<String, WeaponTargetDamage> keyToWtd = new HashMap<>();

        for (Damage td : damages) {

            // don't care about friendly fire
            if(td.firingShip.owner == td.targetShip.owner){
                continue;
            }

            String key = td.firingShip.id + td.targetShip.id + td.weaponName;
            WeaponTargetDamage wtd = keyToWtd.get(key);
            if(wtd == null){
                wtd = new WeaponTargetDamage(combatId, td.firingShip, td.targetShip, td.weaponName);
                keyToWtd.put(key, wtd);
                log.debug("Creating new WeaponTargetDamage entry for: " + td.targetShip +"-"+td.weaponName);
            }

            wtd.merge(td);
        }

        WeaponTargetDamage[] ret = keyToWtd.values().toArray(new WeaponTargetDamage[]{});

        Arrays.sort(ret);

        computePctDamageToTargets(ret);

        return ret;
    }

    public static void computePctDamageToTargets(WeaponTargetDamage[] wtds){
        Map<Ship, Double> shipToTotalDamage = new HashMap<>();
        // build up our totals
        for(WeaponTargetDamage wtd : wtds){
            if(!wtd.target.status.wasKilled()){
                continue;
            }

            Double d = shipToTotalDamage.get(wtd.target);
            if(d == null){
                d = 0d;
            }

            d += wtd.hullDamage + wtd.armorDamage;
            shipToTotalDamage.put(wtd.target, d);
        }

        // assign pcts
        for(WeaponTargetDamage wtd : wtds){
            Double totalDmgToShip = shipToTotalDamage.get(wtd.target);
            if(totalDmgToShip == null){
                continue;
            }

            if(totalDmgToShip >= 1) {
                wtd.pctOfDamageDoneToTarget = (wtd.hullDamage + wtd.armorDamage) / totalDmgToShip;
            }
        }

    }

    // generate a tab separated row from this object.  Use tab as comma could be used for some locales
    public String toTsv(){
        return Helpers.toTsv(combatId, faction, fleetName, ISO_DATE_TIME.format(new Date(engagementEndTime)), combatDurationSeconds);
    }

    public static String getTsvHeader(){
        return Helpers.toTsv("BattleId", "Faction", "FleetName", "BattleTime", "DurationInSeconds");
    }

    public String serialize(){
       return Helpers.toTsv(combatId, faction, fleetName, engagementEndTime, combatDurationSeconds, enemyFleetGoal);
    }

    @Override
    public int compareTo(CombatResult combatResult) {
        // descending is natural sort
        return Long.compare(combatResult.engagementEndTime, this.engagementEndTime);
    }
}
