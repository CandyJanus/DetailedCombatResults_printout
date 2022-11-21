package data.scripts;


import com.fs.starfarer.api.Global;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Serializes DamageReport data onto the data bus that is `Global.getCombatEngine().getCustomData()`
 * No actual custom objects are shared between mods to avoid strange dependency issues.
 *
 * DamageReportManager is a facade over the data bus for ease of use.
 */
public class DamageReportManagerV1 {

    private static final String DamageReportManagerKey = "DamageReportManagerV1";

    public static DamageReportManagerV1 getDamageReportManager(){
        Map<String, Object> customData = Global.getCombatEngine().getCustomData();
        Object raw = customData.get(DamageReportManagerKey);
        DamageReportManagerV1 ret;
        if(raw == null){
            raw = new ArrayList<Object[]>(200);
            customData.put(DamageReportManagerKey, raw);
        }

        if(!(raw instanceof List)) {
            throw new RuntimeException("Unknown class for CustomDataKey: '"+ DamageReportManagerKey +"' class: '"+raw.getClass() +"'");
        }

        return new DamageReportManagerV1((List<Object[]>) raw);
    }

    private final List<Object[]> _damageReports;

    private DamageReportManagerV1(List<Object[]> damageReports){
        _damageReports = damageReports;
    }

    public void addDamageReport(DamageReportV1 dr){
        _damageReports.add(DamageReportV1.serialize(dr));
    }

    public List<DamageReportV1> getDamageReports(){
        if(_damageReports.size() == 0){
            return (List<DamageReportV1>)Collections.EMPTY_LIST;
        }

        List<DamageReportV1> ret = new ArrayList<>(_damageReports.size());
        for(Object[] raw : _damageReports){
            ret.add(DamageReportV1.deserialize(raw));
        }

        return ret;
    }

    /**
     * Must be called at the start of every frame to ensure that if there are no consumers, we don't fill up memory.
     */
    public void clearDamageReports(){
        _damageReports.clear();
    }
}
