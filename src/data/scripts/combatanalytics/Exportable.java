package data.scripts.combatanalytics;

public interface Exportable {
    // generate a tab separated row from this object.  Use tab as comma could be used for some locales
    String toTsv();

    // also must include the static method:  public static String getTsvHeader();
}
