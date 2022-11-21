package data.scripts.combatanalytics.util;

import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.Assert;
import org.junit.Test;

import static data.scripts.combatanalytics.util.Helpers.INT_FORMAT;
import static data.scripts.combatanalytics.util.Helpers.coalesce;

public class HelpersTest {

    @Test
    public void TestParseArrayBlock(){
        String[] values = Helpers.parseArrayBlock("[able, baker  ,charlie]");
        Assert.assertArrayEquals(new String[]{"able", "baker", "charlie"}, values);

        values = Helpers.parseArrayBlock("[]");
        Assert.assertArrayEquals(new String[]{}, values);

        values = Helpers.parseArrayBlock("[     ]");
        Assert.assertArrayEquals(new String[]{}, values);

        values = Helpers.parseArrayBlock("[able]");
        Assert.assertArrayEquals(new String[]{"able"}, values);
    }

    @Test
    public void TestTokenizeTsv(){
        String[] values = Helpers.tokenizeTsv("able\t baker\tcharlie");
        Assert.assertArrayEquals(new String[]{"able", "baker", "charlie"}, values);

        values = Helpers.tokenizeTsv("able\t baker\tcharlie\t");
        Assert.assertArrayEquals(new String[]{"able", "baker", "charlie", ""}, values);
    }

    @Test
    public void TestCleanTsv(){
        Assert.assertEquals("ab le", Helpers.cleanTsvValue("ab\tle"));
        Assert.assertEquals("able  ", Helpers.cleanTsvValue("able\n\r"));
    }

    @Test
    public void TestToTsv(){
        String values = Helpers.toTsv("able", "baker", "charlie");
        Assert.assertEquals("able\tbaker\tcharlie", values);
    }

    @Test
    public void TestCoalesce(){
        Assert.assertEquals("one", coalesce(null, "one"));
        Assert.assertEquals("one", coalesce(null, null, "one"));
    }
}
