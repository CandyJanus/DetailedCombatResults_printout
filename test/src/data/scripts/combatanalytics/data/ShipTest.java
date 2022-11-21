package data.scripts.combatanalytics.data;

import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.util.Helpers;
import org.junit.Assert;
import org.junit.Test;

public class ShipTest {

    @Test
    public void TestSerializeDeserialize(){
        Ship s1 = new Ship("combatId", "Ship1Id", "name", ShipAPI.HullSize.CAPITAL_SHIP, "my ship", 111.0f, 222.0f, 0, 7, "", "hullId", ShipStatus.NOT_FIELDED, "c1", 333f);
        String s1AsString = s1.serialize();
        Ship s2 = new Ship(Helpers.tokenizeTsv(s1AsString));

        Assert.assertEquals(s1.hullClass, s2.hullClass);
        Assert.assertEquals(s1.id, s2.id);
        Assert.assertEquals(s1.combatId, s2.combatId);
        Assert.assertEquals(s1.name, s2.name);
        Assert.assertEquals(s1.hullSize, s2.hullSize);
        Assert.assertEquals(s1.owner, s2.owner);
        Assert.assertEquals(s1.maxHp, s2.maxHp, .0001);
        Assert.assertEquals(s1.maxFlux, s2.maxFlux, .0001);
        Assert.assertEquals(s1.deploymentPoints, s2.deploymentPoints);
        Assert.assertEquals(s1.captain, s2.captain);
        Assert.assertEquals(s1.hullId, s2.hullId);
        Assert.assertEquals(s1.crew, s2.crew);
        Assert.assertEquals(s1.status, s2.status);
        Assert.assertEquals(s1.captainSprite, s2.captainSprite);
        Assert.assertEquals(s1.remainingHp, s2.remainingHp, .0001);

        Assert.assertEquals(s1AsString, s2.serialize());
    }
}
