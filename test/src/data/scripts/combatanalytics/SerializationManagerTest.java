package data.scripts.combatanalytics;

import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.combatanalytics.data.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class SerializationManagerTest {
    @Before
    public void setUp() {
        SerializationManager.overridePersistentData = new HashMap<>();
    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    @After
    public void tearDown() {
        SerializationManager.overridePersistentData = null;
    }

    @Test
    public void TestSaveAndLoad(){
        Ship player = new Ship("player", "combatId", "playership", ShipAPI.HullSize.CRUISER, "player class", 100f, 200f, 1, 7, "cap1", "hullId1",  ShipStatus.DESTROYED, "c1", 333f);
        Ship compy = new Ship("compy", "combatId", "compy ship", ShipAPI.HullSize.CRUISER, "compy class", 100f, 200f, 0, 8, "cap2", "hullId2", ShipStatus.OK, "c2", 333f);

        Damage d1 = new Damage("combatId", 0.1f, "groupName 1", 1.1f, 2.2f, 3.3f,  4.4f, false, player, compy, 5.5f, 6.6f);
        Damage d2 = new Damage("combatId", 1.9f, "groupName 1", 7.7f, 8.8f, 9.9f, 10.10f, true, player, compy, 11.11f, 12.12f);

        CombatResult cr1 = new CombatResult("combatId", "faction", "fleetname", System.currentTimeMillis(), new Damage[]{d1, d2}, 100f, CombatGoal.ESCAPE, new Ship[]{player, compy});

        SerializationManager.saveCombatResult(cr1);
        List<CombatResult> saved = SerializationManager.getAllSavedCombatResults();
        Assert.assertEquals(1, saved.size());

        CombatResult cr2 = saved.get(0);

        Assert.assertEquals(cr1.engagementEndTime, cr2.engagementEndTime);
        Assert.assertEquals(cr1.faction, cr2.faction);
        Assert.assertEquals(cr1.combatId, cr2.combatId);
        Assert.assertEquals(cr1.enemyFleetGoal, cr2.enemyFleetGoal);
        Assert.assertEquals(cr1.combatDurationSeconds, cr2.combatDurationSeconds, .001);


        WeaponTargetDamage wd1 = cr1.weaponTargetDamages[0];
        WeaponTargetDamage wd2 = cr2.weaponTargetDamages[0];

        Assert.assertEquals(wd1.id, wd2.id);
        Assert.assertEquals(wd1.target.id, wd2.target.id);
        Assert.assertEquals(wd1.weapon, wd2.weapon);
        Assert.assertEquals(wd1.hits, wd2.hits);
        Assert.assertEquals(wd1.killingBlow, wd2.killingBlow);
        Assert.assertEquals(wd1.armorDamage, wd2.armorDamage, .0001);
        Assert.assertEquals(wd1.shieldDamage, wd2.shieldDamage, .0001);
        Assert.assertEquals(wd1.hullDamage, wd2.hullDamage, .0001);
        Assert.assertEquals(wd1.empDamage, wd2.empDamage, .0001);
        Assert.assertEquals(wd1.combatId, wd2.combatId);
        Assert.assertEquals(wd1.pctOfDamageDoneToTarget, wd2.pctOfDamageDoneToTarget, .0001);


        Ship s1 = wd1.source;
        Ship s2 = wd2.source;

        Assert.assertEquals(s1.hullClass, s2.hullClass);
        Assert.assertEquals(s1.id, s2.id);
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
    }

    @Test
    public void testCompressDecompress(){
        // verify compress/decompress round trip works
        for(int i=0; i<10000; i++){
            byte[] array = new byte[500];
            Random r = new Random(i);
            r.nextBytes(array);
            String generatedString = new String(array, Charset.forName("UTF-8"));

            String compressed = SerializationManager.compress(generatedString);
            String decompressed = SerializationManager.decompress(compressed);

            Assert.assertEquals(generatedString, decompressed);
        }
    }

}
