package data.scripts.combatanalytics;

import com.fs.starfarer.api.Global;
import data.scripts.combatanalytics.data.CombatGoal;
import data.scripts.combatanalytics.data.CombatResult;
import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.data.WeaponTargetDamage;
import data.scripts.combatanalytics.util.Base64;
import data.scripts.combatanalytics.util.CompressionUtil;
import data.scripts.combatanalytics.util.Helpers;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static data.scripts.combatanalytics.util.Helpers.TWO_DIGIT_NO_GROUP_FORMAT;

/*
 * Manages the serialization and deserialization of data in a string format to prevent breakage when the mod is removed
 * or when the data format changes (allows for migration)
 */
public class SerializationManager {

    private static final String COMBATRESULTS_KEY = "CombatAnalytics_CombatResults_V3";
    private static final String SHIP_KEY = "CombatAnalytics_Ships_V3";
    private static final String WEAPONDAMAGE_KEY = "CombatAnalytics_WeaponDamage_V3";

    private static final String COMBATRESULTS_KEY_OLD = "CombatAnalytics_CombatResults_V2";
    private static final String SHIP_KEY_OLD = "CombatAnalytics_Ships_V2";
    private static final String WEAPONDAMAGE_KEY_OLD = "CombatAnalytics_WeaponDamage_V2";

    private final static Logger log = Global.getLogger(data.scripts.combatanalytics.SerializationManager.class);
    static Map<String, Object> overridePersistentData = null;  // only set by unit tests

    // data lacks integrity, clear and re-write data
    public static boolean dataIsCorrupt = false;

    private static List<CombatResult> _resultCache = null;

    public static void saveCombatResult(CombatResult cr){
        try {
            List<String> ships = getValues(SHIP_KEY);
            List<String> combatResults = getValues(COMBATRESULTS_KEY);
            List<String> weaponDamage = getValues(WEAPONDAMAGE_KEY);

            combatResults.add(cr.serialize());

            for(WeaponTargetDamage wtd : cr.weaponTargetDamages){
                weaponDamage.add(wtd.serialize());
            }

            for (Ship ship : cr.allShips) {
                ships.add(ship.serialize());
            }

            saveValues(SHIP_KEY, ships);
            saveValues(WEAPONDAMAGE_KEY, weaponDamage);
            saveValues(COMBATRESULTS_KEY, combatResults);

            clearCache();
        }
        catch (Throwable e){
            log.error("Unable to save combat result", e);
            Helpers.printErrorMessage("Unable to save combat result");
        }
    }

    public static void onGameLoad(){
        // clear any data saved in statics
        clearCache();
        dataIsCorrupt = false;

        // upgrade old data if any & possible
        Map<String, Object> pd = getPersistentData();

        if(pd.containsKey(COMBATRESULTS_KEY_OLD)){
            // upgrade
            String oldShips = (String) pd.get(SHIP_KEY_OLD);
            String oldWeaponDamages = (String) pd.get(WEAPONDAMAGE_KEY_OLD);
            String oldCombatResults = (String) pd.get(COMBATRESULTS_KEY_OLD);

            // save as new
            save(SHIP_KEY, oldShips);
            save(WEAPONDAMAGE_KEY, oldWeaponDamages);
            save(COMBATRESULTS_KEY, oldCombatResults);
            clearCache();
        }
        // remove old
        pd.remove(SHIP_KEY_OLD);
        pd.remove(WEAPONDAMAGE_KEY_OLD);
        pd.remove(COMBATRESULTS_KEY_OLD);
    }

    public static void clearSavedData(){
        saveValues(SHIP_KEY, new ArrayList<String>());
        saveValues(COMBATRESULTS_KEY, new ArrayList<String>());
        saveValues(WEAPONDAMAGE_KEY, new ArrayList<String>());

        dataIsCorrupt = false;
        clearCache();
    }

    private static void saveValues(String key, List<String> values){
        StringBuilder sb = new StringBuilder(20000);
        for(String line : values){
            sb.append(line);
            sb.append(System.lineSeparator());
        }

        // serialize as an array of strings
        save(key, sb.toString());
    }

    private static void save(String key, String value){
        value = compress(value);
        getPersistentData().put(key, value);
    }

    private static List<String> getValues(String key){
        String rawValues = (String) getPersistentData().get(key);

        rawValues = decompress(rawValues);

        List<String> splitLines = new ArrayList<>();
        if(rawValues != null){
            for (String line : rawValues.split("\n|\r")){
                if(line.length() == 0){
                    continue;
                }

                splitLines.add(line);
            }
        }

        return splitLines;
    }

    static String compress(String s){
        int originalSize = s.length();
        byte[] compressedString = CompressionUtil.compress(s);
        return Base64.convert(compressedString);
    }

    static String decompress(String s){
        if(s == null){
            return null;
        }

        byte[] bytes = Base64.convert(s);
        return CompressionUtil.decompress(bytes);
    }

    public static long sizeOfSavedDataInBytes(){
        long ret = 0;
        String rawValues = (String) getPersistentData().get(COMBATRESULTS_KEY);
        if(rawValues != null){
            ret += rawValues.length();
        }
        rawValues = (String) getPersistentData().get(SHIP_KEY);
        if(rawValues != null){
            ret += rawValues.length();
        }
        rawValues = (String) getPersistentData().get(WEAPONDAMAGE_KEY);
        if(rawValues != null){
            ret += rawValues.length();
        }

        return ret * 2; // ret is char count, 2 byte chars in Java
    }

    public static List<CombatResult> getSavedCombatResults(Set<String> combatResultIds){
        return getSavedCombatResults(new CombatResultFilter(combatResultIds));
    }

    public static List<CombatResult> getAllSavedCombatResultsNoSimulation(){
        List<CombatResult> allCombats = getAllSavedCombatResults();
        List<CombatResult> ret = new ArrayList<>(allCombats.size());

        for(CombatResult cr : allCombats){
            if(cr.enemyFleetGoal != CombatGoal.SIMULATION){
                ret.add(cr);
            }
        }

        return ret;
    }

    public static void clearCache(){
        _resultCache = null;
    }

    public static List<CombatResult> getAllSavedCombatResults(){
        if(_resultCache == null){
            long loadTimeInMs = System.currentTimeMillis();

            try {
                StringInterner stringInterner = new StringInterner();

                List<String> rawShips = getValues(SHIP_KEY);
                List<String> rawCombatResults = getValues(COMBATRESULTS_KEY);
                List<String> rawWeaponDamages = getValues(WEAPONDAMAGE_KEY);

                List<CombatResult> combatResults = new ArrayList<>();
                Map<String, Map<String, Ship>> combatIdToShips = new HashMap<>();
                Map<String, List<WeaponTargetDamage>> combatIdToWTD = new HashMap<>();

                // have to build up ships first, they don't depend upon other objects
                for (String line : rawShips) {
                    try {
                        String[] cells = Helpers.tokenizeTsv(line);
                        stringInterner.internElements(cells);

                        Ship ship = new Ship(cells);

                        Map<String, Ship> ships = combatIdToShips.get(ship.combatId);
                        if(ships == null){
                            ships = new HashMap<>();
                            combatIdToShips.put(ship.combatId, ships);
                        }
                        ships.put(ship.id, ship);
                    } catch (Throwable t) {
                        dataIsCorrupt = true;
                        log.error("Unable to load SavedCombatResults", t);
                    }
                }

                // WeaponDamages depend upon ships
                for (String line : rawWeaponDamages) {
                    try {
                        String[] cells = Helpers.tokenizeTsv(line);
                        stringInterner.internElements(cells);

                        WeaponTargetDamage wtd = new WeaponTargetDamage(cells, combatIdToShips);

                        List<WeaponTargetDamage> wtdsForCombat = combatIdToWTD.get(wtd.combatId);
                        if(wtdsForCombat == null){
                            wtdsForCombat = new ArrayList<>();
                            combatIdToWTD.put(wtd.combatId, wtdsForCombat);
                        }
                        wtdsForCombat.add(wtd);
                    } catch (Throwable t) {
                        dataIsCorrupt = true;
                        log.error("Unable to load SavedCombatResults", t);
                    }
                }

                // CombatResults depend upon ShipCombatResults
                for (String line : rawCombatResults) {
                    try {
                        String[] cells = Helpers.tokenizeTsv(line);
                        stringInterner.internElements(cells);

                        // sometimes we can have a reference to a combat result that doesn't exist any more.  Not totally sure how it happens, but it does.
                        CombatResult scr = new CombatResult(cells, combatIdToWTD, combatIdToShips);
                        combatResults.add(scr);
                    } catch (Throwable t) {
                        dataIsCorrupt = true;
                        log.error("Unable to load SavedCombatResults", t);
                        Helpers.printErrorMessage("Error loading saved combat results");
                    }
                }

                Collections.sort(combatResults);

                _resultCache = combatResults;

                loadTimeInMs = System.currentTimeMillis() - loadTimeInMs;
                log.info("Loaded " + _resultCache.size() + " prior battle results using " +
                        TWO_DIGIT_NO_GROUP_FORMAT.format(SerializationManager.sizeOfSavedDataInBytes() / (1024 * 1024d)) + "MB of memory" +
                        " in "+loadTimeInMs+"ms");
            }
            catch (Throwable e){
                dataIsCorrupt = true;
                log.error("Unable to load SavedCombatResults", e);
                Helpers.printErrorMessage("Error loading saved combat results");
                return new ArrayList<>();
            }
        }

        return _resultCache;
    }

    private static List<CombatResult> getSavedCombatResults(CombatResultFilter filter){
        List<CombatResult>  all = getAllSavedCombatResults();
        List<CombatResult> ret = new ArrayList<>(all.size());

        for(CombatResult cr : all){
            if(filter.isCombatResultValid(cr.combatId)){
                ret.add(cr);
            }
        }

        return ret;
    }

    private static Map<String, Object> getPersistentData(){
        if(overridePersistentData != null){
            return overridePersistentData;
        }
        return Global.getSector().getPersistentData();
    }

    private static class CombatResultFilter{
        private final Set<String> _combatResultIds;

        CombatResultFilter(Set<String> combatResultIds){
            _combatResultIds = combatResultIds;
        }

        CombatResultFilter(){
            _combatResultIds = null;
        }

        public boolean isCombatResultValid(String combatResult){
            return _combatResultIds == null || _combatResultIds.contains(combatResult);
        }
    }

    private static class StringInterner {
        private final HashMap<String, String> _internMap = new HashMap<>(1000);

        public String intern(String s){
            String ret = _internMap.get(s);
            if(ret == null){
                _internMap.put(s, s);
                ret = s;
            }

            return ret;
        }

        public void internElements(String[] arr){
            for(int i=0; i<arr.length; i++){
                arr[i] = intern(arr[i]);
            }
        }

        @Override
        public String toString() {
            return "StringInterner{" +
                    "InternMapSize=" + _internMap.size() +
                    '}';
        }
    }
}
