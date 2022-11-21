package data.scripts.combatanalytics.util;

import org.lwjgl.util.vector.Vector2f;

public class VectorUtil {
    public static double distance(Vector2f a, Vector2f b){
        float x = a.x - b.x;
        x = x * x;

        float y = a.y - b.y;
        y = y * y;

        return Math.sqrt(x + y);
    }
}
