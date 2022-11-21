package data.scripts.combatanalytics.damagedetection;

import data.scripts.combatanalytics.data.Damage;

public class DamageDetectorResult {
    public String combatId;
    public Damage[] damages;
    public float combatDurationSeconds;

    public boolean wasAutoResolved(){
        return damages == null || damages.length == 0;
    }
}
