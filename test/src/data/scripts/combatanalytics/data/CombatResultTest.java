package data.scripts.combatanalytics.data;

import data.scripts.combatanalytics.util.Helpers;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CombatResultTest {

    @Test
    public void TestSerializeDeserialize(){
        CombatResult cr1 = new CombatResult("combatId", "faction", "fleetname", System.currentTimeMillis(), new Damage[0], 100f, CombatGoal.BATTLE, new Ship[]{});
        String cr1AsString = cr1.serialize();
        HashMap<String, List<WeaponTargetDamage>> map = new HashMap<String, List<WeaponTargetDamage>>();
        map.put(cr1.combatId, new ArrayList<WeaponTargetDamage>());

        Map<String, Map<String, Ship>> combatIdToShips = new HashMap<>();
        combatIdToShips.put("combatId", new HashMap<String, Ship>());

        CombatResult cr2 = new CombatResult(Helpers.tokenizeTsv(cr1AsString), map, combatIdToShips);

        Assert.assertEquals(cr1.engagementEndTime, cr2.engagementEndTime);
        Assert.assertEquals(cr1.faction, cr2.faction);
        Assert.assertEquals(cr1.combatId, cr2.combatId);
        Assert.assertEquals(cr1.enemyFleetGoal, cr2.enemyFleetGoal);
        Assert.assertEquals(cr1.fleetName, cr2.fleetName);
        Assert.assertEquals(cr1.combatDurationSeconds, cr2.combatDurationSeconds, .001);
    }

    @Test
    public void testSort(){
        CombatResult cr1 = new CombatResult("combatId", "faction", "fleetname", 0, new Damage[0], 100f, CombatGoal.BATTLE, new Ship[]{});
        CombatResult cr2 = new CombatResult("combatId", "faction", "fleetname", 1, new Damage[0], 100f, CombatGoal.BATTLE, new Ship[]{});

        Assert.assertEquals(1, cr1.compareTo(cr2));
    }
}
