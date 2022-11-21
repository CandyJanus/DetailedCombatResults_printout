package data.scripts.combatanalytics.util;


import com.fs.starfarer.api.Global;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;


public class LocalizationTest {

//    @Test
    public void BuildLocalization() throws Exception {
        // todo figure this out at test time
        String jsonFile = "C:\\dev\\src\\starsectorcombatanalytics\\CombatAnalyticsMod\\text\\DetailedCombatResultsStrings_en.json";

        String rawJson = new String(Files.readAllBytes(new File(jsonFile).toPath())); // assert exists
        for(String line : rawJson.split("\\r?\\n")){
            line = line.trim();
            if(line.startsWith("\"") && !line.endsWith("{")){
                line = line.substring(1);
                line = line.substring(0, line.indexOf("\""));

                System.out.println(String.format("public static String %s = getResourceString(\"%s\");", line, line));
            }
            if(line.startsWith("#")){
                line = line.substring(1);
                line = line.trim();
                System.out.println();
                System.out.println("//"+line);

            }
        }
    }

    @Test
    public void TestAllFieldsSetWithDefault() throws Exception {
        Global.setSettings(new TestSettingsAPI());
        System.out.println(Localization.Emp);


        Field[] declaredFields = Localization.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                    && java.lang.reflect.Modifier.isPublic(field.getModifiers())) {

                String value = (String) field.get(null);
                Assert.assertFalse(value.contains("Unable to locate"));
            }
        }
    }

}
