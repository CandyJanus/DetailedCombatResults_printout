package data.scripts.combatanalytics;

import org.junit.Assert;
import org.junit.Test;

public class CampaignEventListenerTest {

    @Test
    public void TestNaturalCase(){
        Assert.assertEquals("Able Baker Charlie", CampaignEventListener.toNaturalCase("able baker charlie"));
    }
}
