package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.data.ShipStatus;
import org.apache.log4j.Logger;

public abstract class FrameProcessor {

    private static final Logger log = Global.getLogger(FrameProcessor.class);

    protected final CombatEngineAPI _engine;
    protected final FrameProcessorState _state;
    protected final ListenerManager _listenerManager;

    private long _processingTime = 0;

    protected FrameProcessor(CombatEngineAPI engine, FrameProcessorState state, ListenerManager listenerManager){
        _engine = engine;
        _state = state;
        _listenerManager = listenerManager;
    }

    public final void processFrame(float amount){
        long start = System.currentTimeMillis();
        internalProcessFrame(amount);
        _processingTime += System.currentTimeMillis() - start;
    }

    public abstract void internalProcessFrame(float amount);

    public final long getProcessingTime(){
        return _processingTime;
    }

    public boolean wasKillingBlow(ListenerDamage ld, String weaponName){
        ShipAPI targetShip = ld.target;
        ShipAPI sourceShip = ld.source;

        boolean killingBlow = false;
        if (!targetShip.isAlive() && _state.aliveShipsLastFrameById.containsKey(targetShip.getId())) {
            if (!targetShip.isStationModule()) {
                killingBlow = true;
                log.debug(sourceShip.getName() + "->" + weaponName + " KILLS " + targetShip.getName() + "  " + ld.getRealDamage() + "dmg");
                setKilled(targetShip);
            } else if (!targetShip.getParentStation().isAlive() && _state.aliveShipsLastFrameById.containsKey(targetShip.getParentStation().getId())) {
                // catch killing blows on stations and other ships that die if all modules are killed
                killingBlow = true;
                log.debug(sourceShip.getName() + "->" + weaponName + " KILLS " + targetShip.getParentStation().getName() + "  " + ld.getRealDamage() + "dmg");
                setKilled(targetShip);
            }
        }

        return killingBlow;
    }

    private void setKilled(ShipAPI ship){
        Ship s = _state.getTrackedShip(ship);

        // if this was a campaign battle, this will get set again on combat complete
        s.status = ShipStatus.DESTROYED;
    }

    public abstract String getStatsToString();
}
