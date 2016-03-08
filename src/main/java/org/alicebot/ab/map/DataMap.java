package org.alicebot.ab.map;

import org.alicebot.ab.aiml.AIMLDefault;

import java.util.Map;

public class DataMap extends AIMLMap {
    private final Map<String, String> values;

    public DataMap(String name, Map<String, String> values) {
        super(name);
        this.values = values;
    }

    @Override
    public String get(String key) {
        return values.getOrDefault(key, AIMLDefault.default_map);
    }

    @Override
    public int size() {
        return values.size();
    }

}
