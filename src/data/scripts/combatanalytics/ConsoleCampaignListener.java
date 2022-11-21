package data.scripts.combatanalytics;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.util.container.Pair;

import java.awt.Color;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Used only for logging info on game load because otherwise we'd end up logging before the UI is rendering.
 * Taken from LazyWizard's Console Commands
 */

//nukenote: I changed the import of the pair because it was being fucky wucky?
public class ConsoleCampaignListener implements EveryFrameScript {

    public static void enqueueForLogging(String s, Color c){
        toWrite.add(new Pair<>(s, c));
    }

    private static final ConcurrentLinkedQueue<Pair<String, Color>> toWrite = new ConcurrentLinkedQueue<>();

    private static final int PollInterval = 1;

    private float currentInterval = 0f;

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
        currentInterval += amount;

        if(currentInterval > PollInterval){
            while(!toWrite.isEmpty()){
                Pair<String, Color> logMe = toWrite.poll();
                Global.getSector().getCampaignUI().addMessage(logMe.one, logMe.two);
            }

            currentInterval = 0f;
        }
    }
}
