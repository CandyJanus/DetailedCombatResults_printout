package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListenerManager {

    private final HashMap<String, DamageDetectorListener> _shipIdToListener = new HashMap<>(100);

    void ensureShipsHaveListeners(List<ShipAPI> ships){
        // ensure all alive ships have a listener to watch for damage
        for(ShipAPI ship : ships){
            if(!ship.isAlive()){
                continue;
            }

            if(!_shipIdToListener.containsKey(ship.getId())){
                ship.removeListenerOfClass(DamageDetectorListener.class);
                DamageDetectorListener listener = new DamageDetectorListener(ship);
                ship.addListener(listener);
                _shipIdToListener.put(ship.getId(), listener);
            }
        }
    }

    void clearState(){
        // clear results for next frame
        for (DamageDetectorListener listener : new ArrayList<>(_shipIdToListener.values())){
            listener.clearDamageResults();
        }
    }

    // ships that have been killed, dispose of their listener
    void handleDestroyedShips(Collection<ShipAPI> ships){
        for(ShipAPI ship : ships){
            DamageDetectorListener listener = _shipIdToListener.remove(ship.getId());
            if(listener != null) {
                listener.dispose();
            }
        }
    }

    // listeners store the damages done, navigate to the appropriate listener and get the damages (if any)
    Map<SourceTargetDamageType, List<ListenerDamage>> getListenerDamages(boolean isBeam){

        Map<SourceTargetDamageType, List<ListenerDamage>> ret = new HashMap<>();
        for(DamageDetectorListener listener : _shipIdToListener.values()){
            for(Map.Entry<SourceTargetDamageType, List<ListenerDamage>>  entry: listener.getAllDamages(isBeam).entrySet()){
                ret.put(entry.getKey(), entry.getValue());
            }
        }

        return ret;
    }

    List<ReportableDamage> getBallisticDamages(){
        List<ReportableDamage> ret = new ArrayList<>();
        for(DamageDetectorListener listener : _shipIdToListener.values()){
            ret.addAll(listener.getBallisticDamages());
        }

        return ret;
    }

    public void dispose(){
        // dispose of our listeners that we were using
        for(DamageDetectorListener listener : _shipIdToListener.values()){
            listener.dispose();
        }

        _shipIdToListener.clear();
    }
}
