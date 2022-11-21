package data.scripts.combatanalytics.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUMap<K, V> extends LinkedHashMap<K, V> {
    private final int entryLimit;

    public LRUMap(int entryLimit, int mapSize) {
        super(mapSize, 0.75f, true);
        this.entryLimit = entryLimit;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() >= entryLimit;
    }
}
