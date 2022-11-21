package data.scripts.combatanalytics.data;

import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.util.Helpers;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class WeaponTargetDamageTest {

    @Test
    public void TestSerializeDeserialize(){
        Ship player = new Ship("player", "combatId", "playership", ShipAPI.HullSize.CRUISER, "player class", 100f, 200f, 1, 7, "cap1", "hullId1",  ShipStatus.DESTROYED, "c1", 333f);
        Ship compy = new Ship("compy", "combatId", "compy ship", ShipAPI.HullSize.CRUISER, "compy class", 100f, 200f, 0, 8, "cap2", "hullId2", ShipStatus.OK, "c2", 333f);
        Map<String, Ship> idToShip = new HashMap<>();
        idToShip.put(player.id, player);
        idToShip.put(compy.id, compy);

        Map<String, Map<String, Ship>> combatToShips = new HashMap<>();
        combatToShips.put("combatId", idToShip);

        WeaponTargetDamage wd1 = new WeaponTargetDamage("combatId", player, compy, "groupName 1");
        wd1.merge(new Damage("combatId", 0, "groupName 1", 0, 0, 100, 0, false, player, compy, 0, 0));
        String wd1Serialized = wd1.serialize();
        WeaponTargetDamage wd2 = new WeaponTargetDamage(Helpers.tokenizeTsv(wd1Serialized), combatToShips);

        Assert.assertEquals(wd1.id, wd2.id);
        Assert.assertEquals(wd1.target.id, wd2.target.id);
        Assert.assertEquals(wd1.source.id, wd2.source.id);
        Assert.assertEquals(wd1.weapon, wd2.weapon);
        Assert.assertEquals(wd1.hits, wd2.hits);
        Assert.assertEquals(wd1.killingBlow, wd2.killingBlow);
        Assert.assertEquals(wd1.armorDamage, wd2.armorDamage, .0001);
        Assert.assertEquals(wd1.shieldDamage, wd2.shieldDamage, .0001);
        Assert.assertEquals(wd1.hullDamage, wd2.hullDamage, .0001);
        Assert.assertEquals(wd1.empDamage, wd2.empDamage, .0001);
        Assert.assertEquals(wd1.combatId, wd2.combatId);
        Assert.assertEquals(wd1.pctOfDamageDoneToTarget, wd2.pctOfDamageDoneToTarget, .0001);

    }

    @Test
    public void TestCompare(){
        Ship player = new Ship("player", "combatId", "playership", ShipAPI.HullSize.CRUISER, "player class", 100f, 200f, 1, 7, "cap1", "hullId1",  ShipStatus.DESTROYED, "c1", 333f);
        Ship compy = new Ship("compy", "combatId", "compy ship", ShipAPI.HullSize.CRUISER, "compy class", 100f, 200f, 0, 8, "cap2", "hullId2", ShipStatus.OK, "c2", 333f);

        WeaponTargetDamage wd1 = new WeaponTargetDamage("c1", player, compy, "groupName 1");
        WeaponTargetDamage wd2 = new WeaponTargetDamage("c1", player, compy, "groupName 2");


        Assert.assertTrue(wd1.compareTo(wd2) > 0);
    }

}
