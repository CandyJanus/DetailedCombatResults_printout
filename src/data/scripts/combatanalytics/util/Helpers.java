package data.scripts.combatanalytics.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.ConsoleCampaignListener;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// Random global things that don't fit well anywhere else since they're used everywhere.
public class Helpers {

    private static final Logger log = Global.getLogger(data.scripts.combatanalytics.util.Helpers.class);

    public static final String NO_WEAPON = "Unknown";

    // Baffling that String.format doesn't support localization formatting of numbers.
    public static final NumberFormat INT_FORMAT = NumberFormat.getInstance();

    public static final NumberFormat TWO_DIGIT_NO_GROUP_FORMAT = NumberFormat.getInstance();
    public static final NumberFormat INT_FORMAT_NO_GROUP_FORMAT = NumberFormat.getInstance();

    public static final SimpleDateFormat ISO_DATE_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // need a general fudge-factor for trying to simulate the inner workings of damage calculations
    public static final float DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY = .40f;

    // dealing with armor is hard, so we have to up the allowable variance for equality
    public static final float DAMAGE_MAX_PCT_DELTA_FOR_EQUALITY_WITH_ARMOR = .55f;


    static{
        INT_FORMAT.setMaximumFractionDigits(0);

        TWO_DIGIT_NO_GROUP_FORMAT.setMaximumFractionDigits(2);
        TWO_DIGIT_NO_GROUP_FORMAT.setGroupingUsed(false);

        INT_FORMAT_NO_GROUP_FORMAT.setMaximumFractionDigits(0);
        INT_FORMAT_NO_GROUP_FORMAT.setGroupingUsed(false);
    }


    public static float parseFloat(String s){
        // if we serialize NaN or Infinity, treat them as 0.
        if(s.length() == 1){
            char c = s.charAt(0);
            if(!Character.isDigit(c)){
                s = "0";
            }
        }

        return Float.parseFloat(s);
    }

    public static String ownerAsString(int owner){
        switch (owner)
        {
            case 0: return "Player";
            case 1: return "Computer";
            default: return "Unknown Owner Value: " + owner;
        }
    }

    // tab separated row, use tabs since no numberformats I'm aware of use that char
    public static String toTsv(Object... objs){
        StringBuilder sb = new StringBuilder(100);
        for(int i=0; i < objs.length; i++){
            Object o = objs[i];
            if(o == null){
                throw new NullPointerException("Cannot serialize NULL value at index " + i);
            }

            String val;
            if(o instanceof Float){ //Type specific handling here if necessary
                val = o.toString();
            } else {
                val = o.toString();
            }

            val = cleanTsvValue(val);

            sb.append(val);
            sb.append('\t');
        }

        sb.setLength(sb.length()-1); // remove last tab

        return sb.toString();
    }

    public static String[] tokenizeTsv(String line){
        String[] ret = line.split("\t", -1);
        for(int i=0; i<ret.length; i++){
            ret[i] = ret[i].trim();
        }

        return ret;
    }

    public static String cleanTsvValue(String cell){
        return cell.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    public static String encodeArrayBlock(List<String> array){
        StringBuilder sb = new StringBuilder(5000);
        sb.append('[');
        for(int i=0; i<array.size(); i++){
            sb.append(array.get(i));
            if(i+1 < array.size()){
                sb.append(',');
            }
        }
        sb.append(']');

        return sb.toString();
    }

    public static String[] parseArrayBlock(String arr){

        if(arr.startsWith("[")){
            arr = arr.substring(1);
        }
        if(arr.endsWith("]")){
            arr = arr.substring(0, arr.length() - 1);
        }

        arr = arr.trim();

        if(arr.length() == 0){
            return new String[0];
        }

        if(arr.indexOf(',') == -1){
            return new String[]{arr};
        }

        String[] ret = arr.split(",", -1);
        for(int i=0; i<ret.length; i++){
            ret[i] = ret[i].trim();
        }

        return ret;
    }

    @SafeVarargs
    public static <T> T coalesce(T... values){

        for(T value : values){
            if(value != null){
                return value;
            }
        }

        return null;
    }

    private static String UUIDTimeLow(String uuid){
        int dashIndex = uuid.indexOf('-');
        if(dashIndex > -1){
            return uuid.substring(0, dashIndex);
        }

        return uuid;
    }

    // used when we want a unique value, but don't want it to be HUGE (file size reasons).  Probably enough entropy for what we're doing.
    public static String getSmallUuid() {
        return UUIDTimeLow(java.util.UUID.randomUUID().toString());
    }

    public static void printErrorMessage(String message){
        ConsoleCampaignListener.enqueueForLogging("Detailed Combat Results: "+message+"  See log for details.", Color.RED);
    }

    @SafeVarargs
    public static <E> List<E> concat(Collection<E>... listsOfThings){
        List<E> ret = new ArrayList<>();
        for(Collection<E> things : listsOfThings){
            if(things != null) {
                ret.addAll(things);
            }
        }

        return ret;
    }

    public static float computePctDeltaForDamage(float a, float b){
        if(a < .5f && b < .5f){
            return 0f;
        }

        float delta = Math.abs(a - b);
        if(delta < 3){
            return 0f;
        }

        return delta / (a + b);
    }

    public static float getSourceDamageScalar(ShipAPI source, ShipAPI target, DamageType dt, boolean isBeam){
        float ret = 1.0f;
        MutableShipStatsAPI stats = source.getMutableStats();

        switch (target.getHullSize()){
            case DEFAULT:
                break;
            case FIGHTER:
                ret *= stats.getDamageToFighters().getModifiedValue();
                break;
            case FRIGATE:
                ret *= stats.getDamageToFrigates().getModifiedValue();
                break;
            case DESTROYER:
                ret *= stats.getDamageToDestroyers().getModifiedValue();
                break;
            case CRUISER:
                ret *= stats.getDamageToCruisers().getModifiedValue();
                break;
            case CAPITAL_SHIP:
                ret *= stats.getDamageToCapital().getModifiedValue();
                break;
        }

        if(isBeam) {
            ret *= stats.getBeamWeaponDamageMult().getModifiedValue();
        }

        switch (dt){
            case ENERGY:
                ret *= stats.getEnergyWeaponDamageMult().getModifiedValue();
                break;
            case OTHER:
                break;
        }

        return ret;
    }

    public static String formatAsPercent(double f) {
        return INT_FORMAT.format(f * 100) + "%";
    }

    public static String getUnknownWeaponName(DamageType dt){
        return "Unknown " + dt.getDisplayName();
    }

    public static String getUnknownWeaponName(DamageType dt, String weaponType){
        return getUnknownWeaponName(dt) + " " + weaponType;
    }

    public static String getCollisionWeaponName(DamageType dt){
        switch (dt) {
            case HIGH_EXPLOSIVE:
            case KINETIC:
            case FRAGMENTATION:
                return "Collision - Hull";
            case ENERGY:
                return "Collision - Shield";
        }

        return "Unknown Collision Type";
    }
}
