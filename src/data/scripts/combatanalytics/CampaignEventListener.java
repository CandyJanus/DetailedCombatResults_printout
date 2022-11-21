package data.scripts.combatanalytics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.combatanalytics.damagedetection.DamageDetectorResult;
import data.scripts.combatanalytics.damagedetection.EveryFrameDamageDetector;
import data.scripts.combatanalytics.data.CombatGoal;
import data.scripts.combatanalytics.data.CombatResult;
import data.scripts.combatanalytics.data.Damage;
import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.data.ShipStatus;
import data.scripts.combatanalytics.util.Helpers;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static data.scripts.combatanalytics.util.Helpers.concat;

public class CampaignEventListener extends BaseCampaignEventListener {
    private static final Logger log = Global.getLogger(CampaignEventListener.class);

    public CampaignEventListener()
    {
        super(false);
    }

    public void reportPlayerEngagement(EngagementResultAPI result) {
        try {
            log.debug("reportPlayerEngagement");

            DamageDetectorResult detectorResult = EveryFrameDamageDetector.completeCombatAndReset();
            if (detectorResult.wasAutoResolved()) {
                return;    // nothing to display
            }

            CampaignFleetAPI enemyFleet = result.getBattle().getNonPlayerCombined();
            CampaignFleetAPI playerFleet = result.getBattle().getPlayerCombined();

            EngagementResultForFleetAPI playerResult = getPlayerResult(result);
            EngagementResultForFleetAPI enemyResult = getOpponentResult(result);
            CombatGoal combatGoal = getEnemyFleetCombatGoal(playerResult, enemyResult);

            // add our ships that were active in combat (they exchanged damage)
            Map<String, Ship> idToShip = new HashMap<>();
            for(Damage d : detectorResult.damages){
                idToShip.put(d.firingShip.id, d.firingShip);
                idToShip.put(d.targetShip.id, d.targetShip);
            }

            // add our ships that were in the fleet but not involved in hostilities
            addShipsFromFleet(detectorResult.combatId, concat(playerResult.getReserves(), enemyResult.getReserves(), playerResult.getDeployed(), enemyResult.getDeployed()), idToShip);
            Ship[] allShips = idToShip.values().toArray(new Ship[]{});
            Arrays.sort(allShips);
            setFleetMemberData(allShips, result);

            String factionName = toNaturalCase(enemyFleet.getFaction().getDisplayNameLong());
            CombatResult cr = new CombatResult(detectorResult.combatId, factionName, enemyFleet.getName(),
                    System.currentTimeMillis(), detectorResult.damages, detectorResult.combatDurationSeconds, combatGoal, allShips);

            //todo limit combat result count here
            SerializationManager.saveCombatResult(cr);

            IntelCombatReport icr = new IntelCombatReport(enemyFleet.getFaction(), cr);
            Global.getSector().getIntelManager().addIntel(icr);
            log.info("Created combat report:  " + cr.toString());
        } catch (Throwable e){
            log.error("Error in reportPlayerEngagement", e);
            Helpers.printErrorMessage("Error saving combat results");
        }
    }

    private static EngagementResultForFleetAPI getPlayerResult(EngagementResultAPI result){
        if(result.didPlayerWin()){
            return result.getWinnerResult();
        } else {
            return result.getLoserResult();
        }
    }

    private static EngagementResultForFleetAPI getOpponentResult(EngagementResultAPI result){
        if(result.didPlayerWin()){
            return result.getLoserResult();
        } else {
            return result.getWinnerResult();
        }
    }

    // set data on ship that most naturally comes from fleetMember (not ShipApi)
    public static void setFleetMemberData(Ship[] allShips, EngagementResultAPI result){
        // all ships before the battle
        Map<String, FleetMemberAPI> idToFleetMember = new HashMap<>();
        Map<String, ShipAPI> idToShipApi = new HashMap<>();
        for(DeployedFleetMemberAPI dfm : concat(result.getWinnerResult().getAllEverDeployedCopy(), result.getLoserResult().getAllEverDeployedCopy())){
            idToFleetMember.put(dfm.getMember().getId(), dfm.getMember());
            idToShipApi.put(dfm.getMember().getId(), dfm.getShip());
        }
        for(FleetMemberAPI fm : concat(result.getWinnerResult().getReserves(), result.getLoserResult().getReserves())){
            idToFleetMember.put(fm.getId(), fm);
        }

        int winner = 0;
        if(!result.didPlayerWin()){
            winner = 1;
        }

        Set<String> destroyed = new HashSet<>();
        Set<String> disabled = new HashSet<>();
        Set<String> retreated = new HashSet<>();
        Set<String> notFielded = new HashSet<>();
        for(FleetMemberAPI fm : concat(result.getWinnerResult().getDestroyed(), result.getLoserResult().getDestroyed())){
            destroyed.add(fm.getId());
        }

        for(FleetMemberAPI fm : concat(result.getWinnerResult().getDisabled(), result.getLoserResult().getDisabled())){
            disabled.add(fm.getId());
        }

        for(FleetMemberAPI fm : concat(result.getWinnerResult().getRetreated(), result.getLoserResult().getRetreated())){
            retreated.add(fm.getId());
        }

        for(FleetMemberAPI fm : concat(result.getWinnerResult().getReserves(), result.getLoserResult().getReserves())){
            notFielded.add(fm.getId());
        }

        // set our ship fleet values for all ships we've seen this combat
        for(Ship s: allShips){
            FleetMemberAPI fm = idToFleetMember.get(s.id);
            ShipAPI shipAPI = idToShipApi.get(s.id);
            if(fm == null){
                continue;
            }

            ShipStatus ss;
            if(destroyed.contains(fm.getId())){
                ss = ShipStatus.DESTROYED;
            } else if(disabled.contains(fm.getId())){
                ss = ShipStatus.DISABLED;
            } else if(retreated.contains(fm.getId())){
                ss = ShipStatus.RETREATED;
            } else if(notFielded.contains(fm.getId())){
                ss = ShipStatus.NOT_FIELDED;
            } else {
                ss = s.owner == winner ? ShipStatus.OK :  ShipStatus.DESTROYED; // omega sub-ships won't work unless we assume destroyed
            }

            int crew = (int) (fm.getMinCrew() + fm.getMaxCrew()) / 2;
            s.setFleetMemberData(
                    fm.getCaptain(),
                    (int)fm.getUnmodifiedDeploymentPointsCost(),
                    ss,
                    crew,
                    shipAPI == null ? s.maxHp : shipAPI.getHitpoints()
                    );
        }
    }

    public void addShipsFromFleet(String combatId, List<FleetMemberAPI> fleetMembers, Map<String, Ship> shipIdToShip){
        for (FleetMemberAPI fleetMember : fleetMembers){
            if(fleetMember.isFighterWing()){
                continue;
            }

            Ship s = shipIdToShip.get(fleetMember.getId());
            if(s == null){
                s = new Ship(fleetMember, combatId);
                shipIdToShip.put(s.id, s);
            }
        }
    }

    public CombatGoal getEnemyFleetCombatGoal(EngagementResultForFleetAPI player, EngagementResultForFleetAPI enemy){
        if(player.getGoal() == FleetGoal.ESCAPE){
            return CombatGoal.PURSUIT;
        } else if(enemy.getGoal() == FleetGoal.ESCAPE){
            return CombatGoal.ESCAPE;
        } else {
            return CombatGoal.BATTLE;
        }
    }

    // upper case the first letter of words for the faction name.
    public static String toNaturalCase(String name){
        char[] chars = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        for(int i=1; i<chars.length; i++){
            if(chars[i-1] == ' '){
                chars[i] = Character.toUpperCase(chars[i]);
            }
        }

        return new String(chars);
    }
}
