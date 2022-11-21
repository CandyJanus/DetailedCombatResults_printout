package data.scripts.combatanalytics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import data.scripts.combatanalytics.data.CombatResult;
import data.scripts.combatanalytics.util.Localization;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class InteractionDialogPlugin implements com.fs.starfarer.api.campaign.InteractionDialogPlugin {
    private static final Logger log = Global.getLogger(InteractionDialogPlugin.class);
    private static final String NEW_LINE = "\n";

    private static final String Leave = "Leave";
    private static final String Init = "Init";

    private InteractionDialogAPI dialog;
    private TextPanelAPI textPanel;

    private boolean paused;

    public void init(InteractionDialogAPI dialog) {
        paused = Global.getSector().isPaused();

        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        OptionPanelAPI options = dialog.getOptionPanel();
        dialog.hideVisualPanel();

        options.clearOptions();
        textPanel.clear();

        List<CombatResult> combatResults = SerializationManager.getAllSavedCombatResultsNoSimulation();
        Collections.sort(combatResults);

        if (combatResults.size() >= 5) {
            options.addOption(Localization.LastFiveBattles, combatResults.subList(0, 5), null);
        }

        if (combatResults.size() >= 10) {
            options.addOption(Localization.LastTenBattles, combatResults.subList(0, 10), null);
        }

        if (combatResults.size() > 0) {
            options.addOption(Localization.AllBattles, combatResults, null);
        }

        //todo by faction also?

        options.addOption(Leave, Leave, null);
        dialog.setOptionOnEscape(Leave, Leave);


        optionSelected(Init, combatResults);
    }

    public void backFromEngagement(EngagementResultAPI result) {
        // no combat here, so this won't get called
    }

    public void optionSelected(String text, Object optionData) {
        if(text.equals(Init)){
            List<CombatResult> combatResults = (List<CombatResult>) optionData;
            if(combatResults.size() > 0) {
                addText(Localization.ChooseData);
            } else {
                addText(Localization.NoData);
            }
            return;
        }

        // could be here because of dismissal, not just wanting to build an ICR
        if(optionData instanceof List){
            List<CombatResult> combatResults = (List<CombatResult>) optionData;

            IntelCombatReport icr = new IntelCombatReport(combatResults.toArray(new CombatResult[]{}));
            Global.getSector().getIntelManager().addIntel(icr);
            log.debug("Created combat report from " + combatResults.size() + " Combat Results");
        }

        Global.getSector().setPaused(paused);
        dialog.dismiss();
    }

    private void addText(String text) {
        for(String s : text.split(NEW_LINE)){
            textPanel.addParagraph(s);
        }
    }

    public void optionMousedOver(String optionText, Object optionData) {
    }

    public void advance(float amount) {

    }

    public Object getContext() {
        return null;
    }

    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

}
