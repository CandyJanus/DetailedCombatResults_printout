package data.scripts.combatanalytics.damagedetection;

import com.fs.starfarer.api.combat.DamageType;
import org.junit.Assert;
import org.junit.Test;

public class RawDamageTest {

    @Test
    public void testCompareTo(){
        RawDamage bd1 = new RawDamage(5, 6, DamageType.ENERGY);
        RawDamage bd2 = new RawDamage(2, 2, DamageType.ENERGY);

        Assert.assertEquals(1, bd1.compareTo(bd2));
        Assert.assertEquals(-1, bd2.compareTo(bd1));
        Assert.assertEquals(0, bd1.compareTo(bd1));
    }

    @Test
    public void testBasicDamageSortsTheSameAsListenerDamage(){

        Assert.fail();
    }
}
