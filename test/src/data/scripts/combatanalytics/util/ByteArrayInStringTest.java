package data.scripts.combatanalytics.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class ByteArrayInStringTest {

    @Test
    public void TestByteArrayInString(){
        int arraySize = 100000;

        for(int testIndex=0; testIndex<1000; testIndex++){
            Random r = new Random(testIndex);
            byte[] b = new byte[arraySize + testIndex];
            for(int arrIndex = 0; arrIndex < b.length; arrIndex++){
                b[arrIndex] = (byte)r.nextInt(256);
            }

            String toString = Base64.convert(b);
            byte[] backToBytes = Base64.convert(toString);

            Assert.assertEquals(b.length, backToBytes.length);
            for(int i=0; i<b.length; i++){
                Assert.assertEquals("Index: " + i + " for random: " + testIndex, b[i], backToBytes[i]);
            }

            if(testIndex % 100 == 0) {
                System.out.println("Completed: " + testIndex);
            }
        }
    }

}
