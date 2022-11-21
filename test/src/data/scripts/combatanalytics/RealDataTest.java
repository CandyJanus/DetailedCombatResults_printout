package data.scripts.combatanalytics;

import data.scripts.combatanalytics.data.CombatResult;
import data.scripts.combatanalytics.function.AggregateProcessor;
import data.scripts.combatanalytics.function.GroupedByShipDamage;
import data.scripts.combatanalytics.util.Base64;
import data.scripts.combatanalytics.util.CompressionUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RealDataTest {

    @Test
    public void testFightersKilledByWeapons() {
        List<CombatResult> saved = SerializationManager.getAllSavedCombatResults();
        Assert.assertEquals(2, saved.size());

        GroupedByShipDamage[] aggregateStats = AggregateProcessor.aggregateWeaponDamageByShip(saved);

        for(GroupedByShipDamage sd : aggregateStats) {
            if(sd.ship.owner == 0) {
                int count = sd.allDamages.aggregateFighters().getSoloKills().size();
                System.out.println(count);
            }
        }
    }

    @Test
    public void testCompression() throws Exception {
        testCompression("wtd.tsv");
        testCompression("ship.tsv");
        testCompression("cr.tsv");
    }

    public void testCompression(String fileName) throws Exception{
        System.out.println();
        System.out.println(fileName);
        String wtd = getResourceFileContent(fileName);

        System.out.println("StrSize:  " + wtd.length() * 2);
        byte[] compressedString = CompressionUtil.compress(wtd);
        System.out.println("compressed Size: " + compressedString.length);
        String baisCompressed = Base64.convert(compressedString);
        System.out.println("Base64 compressed size: " + baisCompressed.length() * 2);

        String wtd2 = CompressionUtil.decompress(compressedString);

        Assert.assertEquals(wtd, wtd2);
    }


    @Before
    public void setUp() throws Exception {
        Map<String, Object> savedData = new HashMap<>();
        savedData.put("CombatAnalytics_WeaponDamage_V3", SerializationManager.compress(getResourceFileContent("wtd.tsv")));

        savedData.put("CombatAnalytics_Ships_V3", SerializationManager.compress(getResourceFileContent("ship.tsv")));

        savedData.put("CombatAnalytics_CombatResults_V3", SerializationManager.compress(getResourceFileContent("cr.tsv")));


        SerializationManager.overridePersistentData = savedData;
    }

    private String getResourceFileContent(String fileName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        Assert.assertTrue(file.exists());

        return new String(Files.readAllBytes(file.toPath()));
    }

    /**
     * Tears down the test fixture.
     * (Called after every test case method.)
     */
    @After
    public void tearDown() {
        SerializationManager.overridePersistentData = null;
    }
}
