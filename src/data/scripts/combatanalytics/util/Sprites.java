package data.scripts.combatanalytics.util;

import com.fs.starfarer.api.Global;

public class Sprites {
    public static final String Outline = getSprite("character_portrait_outline");
    public static final String PlayerShipIndicator = getSprite("player_ship_icon");
    public static final String Destroyed = getSprite("destroyed");
    public static final String Disabled = getSprite("disabled");
    public static final String Retreated = getSprite("retreated");

    public static String getDamageSpriteForDmgPct(double pctDamage){

        int[] cutPoints = new int[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        int span = 5;

        int intPct = (int) (pctDamage * 100);

        int spriteSize = cutPoints[0];
        for (int cutPoint : cutPoints){
            if(intPct > cutPoint - 5 && intPct <= cutPoint +5){
                spriteSize = cutPoint;
                break;
            }
        }

        return getSprite("damage_"+spriteSize);
    }

    private static String getSprite(String name){
        try{
            return Global.getSettings().getSpriteName("detailedcombatresults", name);
        } catch(Throwable t){
            throw new RuntimeException("Unable to locate the sprite 'detailedcombatresults."+name+"'");
        }
    }
}
