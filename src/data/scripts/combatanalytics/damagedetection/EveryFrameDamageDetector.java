package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import data.scripts.combatanalytics.data.Damage;
import data.scripts.combatanalytics.util.Helpers;
import data.scripts.combatanalytics.util.Settings;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static data.scripts.combatanalytics.util.Helpers.TWO_DIGIT_NO_GROUP_FORMAT;

// main combat damage data acquisition orchestration point
public class EveryFrameDamageDetector {
    private static final Logger log = Global.getLogger(EveryFrameDamageDetector.class);

    private static final boolean DebugEnabled = log.isDebugEnabled();
    private static EveryFrameDamageDetector DamageDetector = null;

    public static void init(CombatEngineAPI engine){  // Called from DamageDetectionEveryFrameCombatPlugin (in scripts)
        DamageDetector = new EveryFrameDamageDetector(engine);
        log.debug("Initializing EveryFrameDamageDetector");
    }

    public static void detectDamage(float amount) { // Called from DamageDetectionEveryFrameCombatPlugin (in scripts)
        if (DamageDetector == null) {
            return;
        }

        DamageDetector.handleFrame(amount);
    }

    public static boolean isSimulationComplete(){
        // check that there are frame counts as there is a brief overlap where the CombatEngine has been created, but the SimulationCompleteListener is still advancing
        return DamageDetector != null && DamageDetector._engine != null && DamageDetector._isSimulation && DamageDetector._frameCount > 0;
    }

    // called when combat is done, returns all of the damages done and resets the internal state
    public static DamageDetectorResult completeCombatAndReset() {
        DamageDetectorResult ret = new DamageDetectorResult();
        if (DamageDetector == null || DamageDetector._engine == null) {
            return ret;
        }

        ret.damages = DamageDetector.getCombatDamages();
        ret.combatDurationSeconds = DamageDetector.getCombatDurationInSeconds();
        ret.combatId = DamageDetector._combatId;

        log.info(DamageDetector.getStatsToString());
        log.info(DamageDetector._projectileProcessor.getStatsToString());
        log.info(DamageDetector._beamProcessor.getStatsToString());
        log.info(DamageDetector._unclaimedProcessor.getStatsToString());
        log.info(DamageDetector._destroyedMissilesProcessor.getStatsToString());
        log.info(DamageDetector._reportedDamageProcessor.getStatsToString());
        log.info(DamageDetector._state.getStatsToString());

        DamageDetector.dispose();
        DamageDetector = null;
        log.debug("Destroying EveryFrameDamageDetector");

        return ret;
    }


    private CombatEngineAPI _engine;
    private final boolean _isSimulation;
    public final String _combatId = Helpers.getSmallUuid();

    private FrameProcessorState _state;

    private FrameProcessorBeam _beamProcessor;
    private FrameProcessorProjectile _projectileProcessor;
    private FrameProcessorUnclaimed _unclaimedProcessor;
    private FrameProcessorReportedDamage _reportedDamageProcessor;
    private FrameProcessorDestroyedMissiles  _destroyedMissilesProcessor;

    private ListenerManager _listenerManager;

    private long _elapsedSystemTime = 0;
    private long _longFrameCount = 0; // frames that actually took long enough to process that it slowed things down.
    private long _frameCount = 0;


    private EveryFrameDamageDetector(CombatEngineAPI engine){
        _isSimulation = engine.isSimulation() && !engine.isMission();
        if(engine.isMission()){ // if these are true, this object won't be properly disposed of so don't set it
            _engine = null;
        } else {
            _engine = engine;
            _state = new FrameProcessorState(_combatId);
            _listenerManager = new ListenerManager();
            _beamProcessor = new FrameProcessorBeam(engine, _state, _listenerManager);
            _projectileProcessor = new FrameProcessorProjectile(engine, _state, _listenerManager);
            _unclaimedProcessor = new FrameProcessorUnclaimed(engine, _state, _listenerManager);
            _reportedDamageProcessor = new FrameProcessorReportedDamage(engine, _state, _listenerManager);
            _destroyedMissilesProcessor = new FrameProcessorDestroyedMissiles(engine, _state, _listenerManager);
        }
    }

    private float getCombatDurationInSeconds(){
        return _engine == null ? 0 : _engine.getTotalElapsedTime(false);
    }

    public void handleFrame(float amount) { // Called from DamageDetectionEveryFrameCombatPlugin
        if (_engine == null){
            return;
        }

        long frameTime = System.currentTimeMillis();
        try {
            _listenerManager.ensureShipsHaveListeners(_engine.getShips());
            _state.updateCommonState(amount, _engine);

            _projectileProcessor.processFrame(amount);
            _beamProcessor.processFrame(amount);
            _destroyedMissilesProcessor.processFrame(amount);
            _unclaimedProcessor.processFrame(amount);
            _reportedDamageProcessor.processFrame(amount);

            // clear listeners for next frame
            _listenerManager.clearState();
            _listenerManager.handleDestroyedShips(_state.killedShipsThisFrameById.values());
        } catch (Throwable e) {
            log.error("Error processing damage detection", e);
        }

        frameTime = System.currentTimeMillis() - frameTime;
        if(frameTime > 5){
            _longFrameCount++;
        }

        _elapsedSystemTime +=  frameTime;
        _frameCount++;
    }

    Damage[] getCombatDamages(){
        List<ReportableDamage> reportableDamages = new ArrayList<>();
        reportableDamages.addAll(_reportedDamageProcessor.getCombatDamages());

        // if we should only report on reported damages
        if(!Settings.UseReportedDamagesOnly()){
            reportableDamages.addAll(_beamProcessor.getCombatDamages());
            reportableDamages.addAll(_projectileProcessor.getCombatDamages());
            reportableDamages.addAll(_unclaimedProcessor.getCombatDamages());
        }

        List<Damage> ret = new ArrayList<>(reportableDamages.size());
        for(ReportableDamage damage : reportableDamages){
            ret.add(new Damage(
                    _combatId,
                    _engine.getTotalElapsedTime(false),
                    damage.weaponName,
                    damage.listenerDamage.shield,
                    damage.listenerDamage.armor,
                    damage.listenerDamage.hull,
                    damage.listenerDamage.emp,
                    damage.wasKillingBlow,
                    _state.getTrackedShip(damage.sourceShip),
                    _state.getTrackedShip(damage.targetShip),
                    damage.targetShip.getHitpoints(),
                    damage.targetShip.getFluxTracker().getMaxFlux() - damage.targetShip.getFluxTracker().getFluxLevel()
            ));
        }

        for(FrameProcessorDestroyedMissiles.InterceptedMissile x : _destroyedMissilesProcessor.getInterceptedMissiles()){
            ret.add(new Damage(
                    _combatId,
                    _engine.getTotalElapsedTime(false),
                    x.interceptionWeaponName,
                    0,
                    0,
                    1,
                    0,
                    true,
                    _state.getTrackedShip(x.interceptionSource),
                    _state.getFakeShipForMissile(x.missileName, x.missileSource.getOriginalOwner()),
                    0,
                    0
            ));
        }

        return ret.toArray(new Damage[]{});
    }

    public String getStatsToString(){
        return "Total time in EveryFrameDamageDetector: " + _elapsedSystemTime + " ms " +
                "Long frame count: " + _longFrameCount +
                " FrameCount: " + _frameCount +
                " Mean Time Per Frame: " + TWO_DIGIT_NO_GROUP_FORMAT.format(_elapsedSystemTime / (double) _frameCount) + "ms";
    }

    public String toString(){
        return getStatsToString();
    }

    void dispose(){
        if(_listenerManager != null) {
            _listenerManager.dispose();
        }
        _listenerManager = null;
        _engine = null;
        _beamProcessor = null;
        _projectileProcessor = null;
        _destroyedMissilesProcessor = null;
        _unclaimedProcessor = null;
        _reportedDamageProcessor = null;
        _state = null;
    }
}