package data.scripts.combatanalytics.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * Represents data found in CombatAnalytics settings.json
 */
public class Settings {

    private static final Logger log = Global.getLogger(data.scripts.combatanalytics.util.Settings.class);

    private static JSONObject settingsJson;

    static {
        try {
            JSONObject settings = Global.getSettings().loadJSON("DetailedCombatResults.json");
            settingsJson = settings.getJSONObject("DetailedCombatResults");
        } catch(Throwable t){
            settingsJson = null;
        }
    }

    public static final String CombatAnalyzeKey = getStringSetting("CombatAnalyzeKey", "L");
    public static final String DetailedCombatResultsLoggingLevel = getStringSetting("DetailedCombatResults_LoggingLevel", Level.INFO.toString());
    public static final int MaxCombatResultCount = getIntegerSetting("MaxCombatResultCount", 50);
    public static final int CombatResultLifetimeInDays = getIntegerSetting("CombatResultLifetimeInDays", 730);
    public static final int AggregateResultLifetimeInDays = getIntegerSetting("AggregateResultLifetimeInDays", 5);
    public static final int SimulationResultLifetimeInDays = getIntegerSetting("SimulationResultLifetimeInDays", 1);
    public static final boolean FighterWeaponBreakout = getBooleanSetting("FighterWeaponBreakout", false);
    public static final int ShipCountLimit = getIntegerSetting("ShipCountLimit", 15);

    private static String getStringSetting(String setting, String defaultValue){
        String ret = defaultValue;
        try {
            ret = settingsJson.getString(setting);
        } catch (Throwable e) {
            String message = "Unable to locate the setting '"+setting+"' in JSON, defaulting to '"+defaultValue+"'";
            log.error(message);
            Helpers.printErrorMessage(message);
        }

        return Helpers.coalesce(ret, defaultValue);
    }

    private static boolean getBooleanSetting(String setting, boolean defaultValue){
        boolean ret = defaultValue;
        try {
            ret = settingsJson.getBoolean(setting);
        } catch (Throwable e) {
            String message = "Unable to locate the setting '"+setting+"' in JSON, defaulting to '"+defaultValue+"'";
            log.error(message);
            Helpers.printErrorMessage(message);
        }

        return ret;
    }

    private static int getIntegerSetting(String setting, int defaultValue){
        int ret = defaultValue;
        try {
            ret = settingsJson.getInt(setting);
        } catch (Throwable e) {
            String message = "Unable to locate the setting '"+setting+"' in JSON, defaulting to '"+defaultValue+"'";
            log.error(message);
            Helpers.printErrorMessage(message);
        }

        return ret;
    }

    public static boolean UseReportedDamagesOnly(){

        try {
            return Global.getSettings().getBoolean("DetailedCombatResults_UseReportedDamagesOnlyV1");
        } catch(Throwable t){
            return false;
        }
    }
}
