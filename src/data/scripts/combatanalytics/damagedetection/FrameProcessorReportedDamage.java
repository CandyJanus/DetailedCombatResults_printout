package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.DamageReportManagerV1;
import data.scripts.DamageReportV1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT_NO_GROUP_FORMAT;

public class FrameProcessorReportedDamage extends FrameProcessor {

    private final List<ReportableDamage> combatDamages = new ArrayList<>();

    protected FrameProcessorReportedDamage(CombatEngineAPI engine, FrameProcessorState state, ListenerManager listenerManager) {
        super(engine, state, listenerManager);
    }

    @Override
    public void internalProcessFrame(float amount) {
        DamageReportManagerV1 damageReportManager = DamageReportManagerV1.getDamageReportManager();
        for (DamageReportV1 damageReport : damageReportManager.getDamageReports()){
            if(!(damageReport.getSource() instanceof ShipAPI && damageReport.getTarget() instanceof ShipAPI)){
                continue;
            }

            ListenerDamage ld = new ListenerDamage(
                damageReport.getDamageType(),
                damageReport.getArmorDamage(),
                damageReport.getHullDamage(),
                damageReport.getShieldDamage(),
                damageReport.getEmpDamage(),
                false,
                (ShipAPI)damageReport.getSource(),
                (ShipAPI)damageReport.getTarget()
            );

            combatDamages.add(new ReportableDamage(damageReport.getWeaponName(), ld, wasKillingBlow(ld, damageReport.getWeaponName())));
        }

        damageReportManager.clearDamageReports();
    }

    @Override
    public String getStatsToString() {
        float shield = 0;
        float armor = 0;
        float emp = 0;
        float hull = 0;
        for(ReportableDamage rd : combatDamages){
            shield += rd.listenerDamage.shield;
            armor += rd.listenerDamage.armor;
            emp += rd.listenerDamage.emp;
            hull += rd.listenerDamage.hull;
        }

        return
                "Damages Reported: " + INT_FORMAT_NO_GROUP_FORMAT.format(combatDamages.size()) +
                        "  Total Shield: " + INT_FORMAT_NO_GROUP_FORMAT.format(shield) +
                        "  Total Armor: " + INT_FORMAT_NO_GROUP_FORMAT.format(armor) +
                        "  Total Hull: " + INT_FORMAT_NO_GROUP_FORMAT.format(hull) +
                        "  Total EMP: " + INT_FORMAT_NO_GROUP_FORMAT.format(emp);
    }

    public Collection<ReportableDamage> getCombatDamages() {
        return combatDamages;
    }
}
