package org.alicebot.ab.map;

import org.alicebot.ab.Bot;
import org.alicebot.ab.aiml.AIMLDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class MutableMap extends AIMLMap {

    private static final Logger logger = LoggerFactory.getLogger(MutableMap.class);

    private final Map<String, String> values = new HashMap<>();

    public MutableMap(String name) {
        super(name);
    }

    @Override
    public String get(String key) {
        return values.getOrDefault(key, AIMLDefault.default_map);
    }

    public String put(String key, String value) {
        return values.put(key, value);
    }

    public void writeMap(Bot bot) {
        logger.info("Writing AIML Map {}", name());
        try {
            Stream<String> lines = values.keySet().stream().map(String::trim).map(p -> p + ":" + get(p).trim());
            Path mapFile = bot.mapsPath.resolve(name() + ".txt");
            Files.write(mapFile, (Iterable<String>) lines::iterator);
        } catch (Exception e) {
            logger.error("writeMap error", e);
        }
    }

    @Override
    public int size() {
        return values.size();
    }

}
