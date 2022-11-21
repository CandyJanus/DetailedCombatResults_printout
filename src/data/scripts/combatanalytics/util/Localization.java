package data.scripts.combatanalytics.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.util.Locale;

public class Localization {

    private static final Logger log = Global.getLogger(data.scripts.combatanalytics.util.Localization.class);
    private static final String DefaultStringsFile = "DetailedCombatResultsStrings_en.json";

    private static JSONObject LocalizationJson;
    private static String LoadedFrom;

    static {
        try {
            String file = "DetailedCombatResultsStrings_" + Locale.getDefault().getLanguage() + ".json";
            LocalizationJson = LoadJson(file);
        } catch (Throwable t){
            log.error("Fatal error attempting to set localization");
            Helpers.printErrorMessage("Fatal error attempting to set localization");
        }
    }

    public static JSONObject LoadJson(String fileName){
        try {
            LoadedFrom = fileName;
            JSONObject settings = Global.getSettings().loadJSON(fileName);
            JSONObject settingsJson = settings.getJSONObject("DetailedCombatResults");

            if(settingsJson == null || settings.length() == 0){
                throw new Exception("JSON localization file: '"+fileName+"' was found but was missing the 'combatAnalytics' section");
            }

            log.info("Using Detailed Combat Report localization file: '"+fileName+"'");
            return settingsJson;
        } catch(Throwable t){
            log.warn("Unable to locate & parse strings file named: '"+fileName+"' using default values from: '"+ DefaultStringsFile +"'", t);
            fileName = DefaultStringsFile;
            try {
                LoadedFrom = fileName;
                JSONObject settings = Global.getSettings().loadJSON(fileName);
                return settings.getJSONObject("DetailedCombatResults");
            } catch (Exception e){ // things are busted if it comes to this
                throw new RuntimeException(e);
            }
        }
    }

    private static String getResourceString(String setting){
        try {
            return LocalizationJson.getString(setting);
        } catch (Throwable e) {
            String message = "Unable to locate the setting '" + setting + "' in JSON: '"+LoadedFrom+"'";
            log.error(message);
            Helpers.printErrorMessage(message);
            return message;
        }
    }

    //InteractionDialogPlugin
    public static String LastFiveBattles = getResourceString("LastFiveBattles");
    public static String LastTenBattles = getResourceString("LastTenBattles");
    public static String AllBattles = getResourceString("AllBattles");
    public static String ChooseData = getResourceString("ChooseData");
    public static String NoData = getResourceString("NoData");

    //IntelCombatReport
    public static String AggregateFactionName = getResourceString("AggregateFactionName");
    public static String SingleCombatInfo = getResourceString("SingleCombatInfo");
    public static String SimulatorInfo = getResourceString("SimulatorInfo");
    public static String MultiIntelInfo = getResourceString("MultiIntelInfo");
    public static String SummaryDetailsHeading = getResourceString("SummaryDetailsHeading");
    public static String TotalDamage = getResourceString("TotalDamage");
    public static String DisplayingTop = getResourceString("DisplayingTop");
    public static String PlayerResultsText = getResourceString("PlayerResultsText");
    public static String CaptainedBy = getResourceString("CaptainedBy");

    //ICR ResultsSummaryGrid
    public static String ShipName = getResourceString("ShipName");
    public static String PilotName = getResourceString("PilotName");
    public static String Class = getResourceString("Class");
    public static String SoloKills = getResourceString("SoloKills");
    public static String KillAssists = getResourceString("KillAssists");
    public static String ProportionalDPDestroyed = getResourceString("ProportionalDPDestroyed");
    public static String PctOfTotalDmg = getResourceString("PctOfTotalDmg");
    public static String HullPct = getResourceString("HullPct");
    public static String ShieldDmg = getResourceString("ShieldDmg");
    public static String ArmorDmg = getResourceString("ArmorDmg");
    public static String HullDmg = getResourceString("HullDmg");
    public static String EmpDmg = getResourceString("EmpDmg");
    public static String TotalRowName = getResourceString("TotalRowName");

    //ICR ShipWeaponGrid
    public static String WeaponName = getResourceString("WeaponName");
    public static String Total = getResourceString("Total");
    public static String Shield = getResourceString("Shield");
    public static String Hull = getResourceString("Hull");
    public static String Armor = getResourceString("Armor");
    public static String Emp = getResourceString("Emp");
    public static String Hits = getResourceString("Hits");
    public static String PctDmg = getResourceString("PctDmg");
    public static String Fighter = getResourceString("Fighter");
    public static String Missile = getResourceString("Missile");

    //ICR Kill Summary Grid
    public static String KillType = getResourceString("KillType");
    public static String DP = getResourceString("DP");
    public static String Solo = getResourceString("Solo");
    public static String Assist = getResourceString("Assist");

    //ICR Enemy Fleet Status
    public static String EnemyFleetStatusTitle = getResourceString("EnemyFleetStatusTitle");
    public static String AggregateFleetName = getResourceString("AggregateFleetName");
    public static String EnemyResultsText = getResourceString("EnemyResultsText");
    public static String Destroyed = getResourceString("Destroyed");
    public static String Disabled = getResourceString("Disabled");
    public static String Retreated = getResourceString("Retreated");
    public static String Reserved = getResourceString("Reserved");

    //ICR Received Summary Grid
    public static String ReceivedSummary = getResourceString("ReceivedSummary");

    //Legend
    public static String LegendName = getResourceString("LegendName");
    public static String SoloDesc = getResourceString("SoloDesc");
    public static String AssistDesc = getResourceString("AssistDesc");
    public static String ProRataDpDesc = getResourceString("ProRataDpDesc");
    public static String DamageDesc = getResourceString("DamageDesc");

    //PlayerFleetGoal
    public static String PlayerGoalPursuit = getResourceString("PlayerGoalPursuit");
    public static String PlayerGoalEscape = getResourceString("PlayerGoalEscape");
    public static String PlayerGoalBattle = getResourceString("PlayerGoalBattle");

    //EnemyFleetGoal
    public static String EnemyGoalPursuit = getResourceString("EnemyGoalPursuit");
    public static String EnemyGoalEscape = getResourceString("EnemyGoalEscape");
    public static String EnemyGoalBattle = getResourceString("EnemyGoalBattle");

    //Intel Name
    public static String CombatReportName = getResourceString("CombatReportName");
    public static String CombatReportTag = getResourceString("CombatReportTag");
    public static String SimulatorReportName = getResourceString("SimulatorReportName");
    public static String SimulatorReportTag = getResourceString("SimulatorReportTag");
}
