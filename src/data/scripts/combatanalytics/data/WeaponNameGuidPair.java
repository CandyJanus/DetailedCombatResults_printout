package data.scripts.combatanalytics.data;

import data.scripts.combatanalytics.Exportable;
import data.scripts.combatanalytics.util.Helpers;


public class WeaponNameGuidPair implements Exportable {

    public String weaponName;
    public String id;

    public WeaponNameGuidPair(String weaponName, String id) {
        this.weaponName = weaponName;
        this.id = id;
    }

    public String toTsv(){
        return Helpers.toTsv(
                id,
                weaponName
        );
    }

    public static String getTsvHeader(){
        return Helpers.toTsv("WeaponId", "WeaponName");
    }
}
