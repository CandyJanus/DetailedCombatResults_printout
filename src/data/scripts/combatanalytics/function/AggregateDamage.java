package data.scripts.combatanalytics.function;

import data.scripts.combatanalytics.data.Ship;
import data.scripts.combatanalytics.data.WeaponTargetDamage;

import java.util.HashMap;
import java.util.Map;

/*
The result of aggregating a DamageSet
 */
public class AggregateDamage implements Comparable<AggregateDamage> {
    public final static double AssistThreshold = .20f;
    public final static double SoloThreshold = 1d - AssistThreshold; // can only be a solo if there are no assists

    public final String groupName;

    private final DamageSummary damageSummary = new DamageSummary();

    private final Map<Ship, DamageSummary> targetsToDamage = new HashMap<>();

    public AggregateDamage(String groupName){
        this.groupName = groupName;
    }

    public void merge(WeaponTargetDamage d){
        damageSummary.merge(d);

        DamageSummary shipDamageSummary = targetsToDamage.get(d.target);
        if(shipDamageSummary == null){
            shipDamageSummary = new DamageSummary();
            targetsToDamage.put(d.target, shipDamageSummary);
        }
        shipDamageSummary.merge(d);
    }

    public void merge(AggregateDamage d){
        damageSummary.merge(d.damageSummary);

        for(Map.Entry<Ship, DamageSummary> targetToDamage : d.targetsToDamage.entrySet()){
            DamageSummary shipDamageSummary = targetsToDamage.get(targetToDamage.getKey());
            if(shipDamageSummary == null){
                shipDamageSummary = new DamageSummary();
                targetsToDamage.put(targetToDamage.getKey(), shipDamageSummary);
            }
            shipDamageSummary.merge(targetToDamage.getValue());
        }
    }

    public double totalRealDamage(){
        return damageSummary.totalRealDamage();
    }

    public double totalAllDamage(){
        return damageSummary.totalAllDamage();
    }

    public double getShieldDamage(){
        return damageSummary.shieldDamage;
    }

    public double getHullDamage(){
        return damageSummary.hullDamage;
    }

    public double getArmorDamage(){
        return damageSummary.armorDamage;
    }

    public double getEmpDamage(){
        return damageSummary.empDamage;
    }

    public double getHitCount(){
        return damageSummary.hits;
    }

    public Map<Ship, DamageSummary> getSoloKills(){
        Map<Ship, DamageSummary> ret = new HashMap<>();
        for(Map.Entry<Ship, DamageSummary> entry : this.targetsToDamage.entrySet()){
            if(entry.getKey().status.wasKilled() && entry.getValue().pctOfDamageDoneToTarget > SoloThreshold) {
                ret.put(entry.getKey(), entry.getValue());
            }
        }

        return ret;
    }

    public Map<Ship, DamageSummary> getAssists(){
        Map<Ship, DamageSummary> ret = new HashMap<>();
        for(Map.Entry<Ship, DamageSummary> entry : this.targetsToDamage.entrySet()){
            if(entry.getKey().status.wasKilled() && entry.getValue().pctOfDamageDoneToTarget > AssistThreshold && entry.getValue().pctOfDamageDoneToTarget <= SoloThreshold) {
                ret.put(entry.getKey(), entry.getValue());
            }
        }

        return ret;
    }

    //Useful for when you want to attribute a kill and won't be caring about assists (like for fighters)
    public Map<Ship, DamageSummary> getMajorityKills(){
        Map<Ship, DamageSummary> ret = new HashMap<>();
        for(Map.Entry<Ship, DamageSummary> entry : this.targetsToDamage.entrySet()){
            if(entry.getKey().status.wasKilled() && entry.getValue().pctOfDamageDoneToTarget > .5) {
                ret.put(entry.getKey(), entry.getValue());
            }
        }

        return ret;
    }

    public double getAllProRataDeploymentPointsDestroyed(){
        return getProRataDeploymentPointsDestroyed(targetsToDamage);
    }

    public double getSoloProRataDeploymentPointsDestroyed(){
        return getProRataDeploymentPointsDestroyed(getSoloKills());
    }

    public double getAssistProRataDeploymentPointsDestroyed(){
        return getProRataDeploymentPointsDestroyed(getAssists());
    }

    private double getProRataDeploymentPointsDestroyed(Map<Ship, DamageSummary> shipToDamageSummary){
        double ret = 0;
        for(Map.Entry<Ship, DamageSummary> shipAndDamage : shipToDamageSummary.entrySet()){
            if(shipAndDamage.getKey().status.wasKilled()){
                ret += shipAndDamage.getKey().deploymentPoints * shipAndDamage.getValue().pctOfDamageDoneToTarget;
            }
        }

        return ret;
    }

    @Override
    public String toString(){
        return this.groupName;
    }

    // Highest damage first
    @Override
    public int compareTo(AggregateDamage o) {
        int ret = Double.compare(o.totalRealDamage(), totalRealDamage());
        if(ret == 0){
            ret = groupName.compareTo(o.groupName);
        }

        return ret;
    }
}
