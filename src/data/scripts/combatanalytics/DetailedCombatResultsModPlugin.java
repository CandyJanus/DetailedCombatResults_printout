package data.scripts.combatanalytics;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import data.scripts.combatanalytics.damagedetection.DamageDetectorResult;
import data.scripts.combatanalytics.damagedetection.EveryFrameDamageDetector;
import data.scripts.combatanalytics.data.CombatGoal;
import data.scripts.combatanalytics.data.CombatResult;
import data.scripts.combatanalytics.data.Damage;
import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.util.Helpers;
import data.scripts.combatanalytics.util.Settings;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailedCombatResultsModPlugin extends BaseModPlugin {

    private static final Logger log = Global.getLogger(DetailedCombatResultsModPlugin.class);

    static{
        String strLevel = Settings.DetailedCombatResultsLoggingLevel;
        Level loggingLevel = Level.toLevel(strLevel, Level.INFO);
        log.setLevel(loggingLevel);
        log.info("DetailedCombatResults referenced, logging level set to: " + loggingLevel);
    }

    @Override
    public void onGameLoad(boolean newGame){

        // transient so on mod disabled we aren't referenced
        // register our listener that gets notified when battles complete
        Global.getSector().addTransientListener(new CampaignEventListener());
        Global.getSector().addTransientScript(new ConsoleCampaignListener());
        Global.getSector().addTransientScript(new SimulationCompleteListener());

        int keyboardKey = Keyboard.KEY_L;

        // lot of crazy just to get a GD key mapping to open a dialogue
        String configKey = Settings.CombatAnalyzeKey.toUpperCase();

        if (Keyboard.getKeyIndex(configKey) != 0) {
            keyboardKey = Keyboard.getKeyIndex(configKey);
        }

        final int openKey = keyboardKey;

        log.info("Keyboard key '"+Keyboard.getKeyName(openKey)+"' will open combat aggregation dialogue");


        Locale currentLocale = Locale.getDefault();

        log.info("Language: '"+currentLocale.getLanguage()+"' Country: '"+currentLocale.getCountry()+"'");

        Global.getSector().addTransientScript(new EveryFrameScript() {
            private boolean activated = false; // only activate the window once per keypress

            @Override
            public void advance(float amount) {
                if (!activated && Keyboard.isKeyDown(openKey))
                {
                    activated = true;
                    Global.getSector().getCampaignUI().showInteractionDialog(new InteractionDialogPlugin(), Global.getSector().getPlayerFleet());
                }
                else if (activated && !Keyboard.isKeyDown(openKey))
                {
                    activated = false;
                }
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public boolean runWhilePaused() {
                return true;
            }
        });

        SerializationManager.onGameLoad();
        List<CombatResult> savedCombatResults = SerializationManager.getAllSavedCombatResults();
        int originalSize = savedCombatResults.size();

        // if we have more CRs than we are supposed to, or there's old simulation data, trim
        if(SerializationManager.dataIsCorrupt
        || removeAgedSimulationData(savedCombatResults) > 0
        || removeOldestEntriesIfOverSize(savedCombatResults) > 0
        ){
            SerializationManager.clearSavedData();
            for(CombatResult cr : savedCombatResults) {
                SerializationManager.saveCombatResult(cr);
            }

            log.info("Trimmed combat results to " + savedCombatResults.size() + " entries from " + originalSize);

            // remove the reports that no longer have data
            removeReportsWithoutData();
        }

        ConsoleCampaignListener.enqueueForLogging("Detailed Combat Result loaded, press '"+configKey+"' for aggregation options", Color.LIGHT_GRAY);
    }

    private void removeReportsWithoutData(){
        try {
            // new ArrayList to avoid CME
            for(IntelInfoPlugin iip : new ArrayList<>(Global.getSector().getIntelManager().getIntel(IntelCombatReport.class))){
                if(iip instanceof IntelCombatReport){
                    IntelCombatReport icp = (IntelCombatReport) iip;
                    if(!icp.isValid()){
                        Global.getSector().getIntelManager().removeIntel(icp);
                    }
                }
            }

            Global.getSector().getIntelManager().removeAllThatShouldBeRemoved();
        } catch (Exception ignored){
        }
    }

    private int removeOldestEntriesIfOverSize(List<CombatResult> combatResults){
        int removedCount = 0;
        int maxCombatResultCount = Settings.MaxCombatResultCount;
        log.info("MaxCombatResultCount: " + maxCombatResultCount);

        // newest first
        Collections.sort(combatResults);
        while(combatResults.size() > maxCombatResultCount) {
            combatResults.remove(combatResults.size() - 1);
            removedCount++;
        }

        return removedCount;
    }

    private int removeAgedSimulationData(List<CombatResult> combatResults){
        int removedCount = 0;
        try {
            HashSet<String> expiredSimulationCombatIds = new HashSet<>();

            for(IntelInfoPlugin iip : Global.getSector().getIntelManager().getIntel(IntelCombatReport.class)){
                if(iip instanceof IntelCombatReport){
                    IntelCombatReport icp = (IntelCombatReport) iip;

                    // don't remove regular combatIds, they might not have reports but they could still be used for aggregation
                    if(icp.shouldRemoveIntel() && icp._isSimulation){
                        expiredSimulationCombatIds.addAll(icp._combatResultIds);
                    }
                }
            }

            for(CombatResult result : new ArrayList<>(combatResults)){
                if(expiredSimulationCombatIds.contains(result.combatId) && result.enemyFleetGoal == CombatGoal.SIMULATION){
                    combatResults.remove(result);
                    removedCount++;
                }
            }
        } catch (Exception ignored){
        }

        return removedCount;
    }

    private static class SimulationCompleteListener implements EveryFrameScript {

        @Override
        public boolean isDone() {
            return false;
        }

        @Override
        public boolean runWhilePaused() {
            return true;
        }

        @Override
        public void advance(float amount) {
            if(EveryFrameDamageDetector.isSimulationComplete()){
                simulationComplete();
            }
        }

        private void simulationComplete(){
            try {
                log.debug("simulationComplete");

                DamageDetectorResult detectorResult = EveryFrameDamageDetector.completeCombatAndReset();
                if (detectorResult.wasAutoResolved()) {
                    return;    // nothing to display
                }

                // add our ships that were active in combat (they exchanged damage)
                Map<String, Ship> idToShip = new HashMap<>();
                for(Damage d : detectorResult.damages){
                    idToShip.put(d.firingShip.id, d.firingShip);
                    idToShip.put(d.targetShip.id, d.targetShip);
                }

                Ship[] allShips = idToShip.values().toArray(new Ship[]{});
                Arrays.sort(allShips);

                CombatResult cr = new CombatResult(detectorResult.combatId, "Simulation Opponent", "",
                        System.currentTimeMillis(), detectorResult.damages, detectorResult.combatDurationSeconds, CombatGoal.SIMULATION, allShips);

                SerializationManager.saveCombatResult(cr);

                IntelCombatReport icr = new IntelCombatReport(Color.GRAY, cr);
                Global.getSector().getIntelManager().addIntel(icr);
                log.info("Created combat report:  " + cr.toString());
            } catch (Throwable e){
                log.error("Error in simulationComplete", e);
                Helpers.printErrorMessage("Error saving simulation combat results");
            }
        }
    }

}