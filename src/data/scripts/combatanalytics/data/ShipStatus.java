package data.scripts.combatanalytics.data;

public enum ShipStatus {
    OK(false),
    RETREATED(false),
    DISABLED(true),
    DESTROYED(true),
    NOT_FIELDED(false);

    private final boolean killed;
    ShipStatus(boolean killed){
        this.killed = killed;
    }

    public boolean wasKilled(){
        return killed;
    }
}
