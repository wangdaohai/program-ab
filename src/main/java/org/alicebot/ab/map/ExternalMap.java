package org.alicebot.ab.map;

import org.alicebot.ab.Sraix;
import org.alicebot.ab.aiml.AIMLDefault;

public class ExternalMap extends AIMLMap {

    private final String host;
    private final String botId;

    public ExternalMap(String name, String host, String botId) {
        super(name);
        this.host = host;
        this.botId = botId;
    }

    @Override
    public String get(String key) {
        String query = name().toUpperCase() + " " + key;
        String response = Sraix.sraix(null, query, AIMLDefault.default_map, null, host, botId, null, "0");
        return response == null ? AIMLDefault.default_map : response;
    }

    @Override
    public int size() {
        return 0;
    }

}
