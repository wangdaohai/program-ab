package org.alicebot.ab;
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * implements AIML Map
 * <p>
 * A map is a function from one string set to another.
 * Elements of the domain are called keys and elements of the range are called values.
 */
public class AIMLMap {

    private static final Logger logger = LoggerFactory.getLogger(AIMLMap.class);

    private final Map<String, String> valueMap = new HashMap<>();
    public String mapName;
    String host; // for external maps
    String botid; // for external maps
    boolean isExternal = false;
    Inflector inflector = new Inflector();

    /**
     * constructor to create a new AIML Map
     *
     * @param name the name of the map
     */
    public AIMLMap(String name) {
        this.mapName = name;
    }

    /**
     * return a map value given a key
     *
     * @param key the domain element
     * @return the range element or a string indicating the key was not found
     */
    public String get(String key) {
        String value;
        if (mapName.equals(MagicStrings.map_successor)) {
            try {
                int number = Integer.parseInt(key);
                return String.valueOf(number + 1);
            } catch (Exception ex) {
                return MagicStrings.default_map;
            }
        } else if (mapName.equals(MagicStrings.map_predecessor)) {
            try {
                int number = Integer.parseInt(key);
                return String.valueOf(number - 1);
            } catch (Exception ex) {
                return MagicStrings.default_map;
            }
        } else if ("singular".equals(mapName)) {
            return inflector.singularize(key).toLowerCase();
        } else if ("plural".equals(mapName)) {
            return inflector.pluralize(key).toLowerCase();
        } else if (isExternal && MagicBooleans.enable_external_sets) {
            //String[] split = key.split(" ");
            String query = mapName.toUpperCase() + " " + key;
            String response = Sraix.sraix(null, query, MagicStrings.default_map, null, host, botid, null, "0");
            logger.info("External {}({})={}", mapName, key, response);
            value = response;
        } else {
            value = valueMap.get(key);
        }
        if (value == null) { value = MagicStrings.default_map; }
        //System.out.println("AIMLMap get "+key+"="+value);
        return value;
    }

    /**
     * put a new key, value pair into the map.
     *
     * @param key   the domain element
     * @param value the range element
     * @return the value
     */
    public String put(String key, String value) {
        //System.out.println("AIMLMap put "+key+"="+value);
        return valueMap.put(key, value);
    }

    public void writeMap(Bot bot) {
        logger.info("Writing AIML Map {}", mapName);
        try {
            Stream<String> lines = valueMap.keySet().stream().map(String::trim).map(p -> p + ":" + get(p).trim());
            File mapFile = new File(bot.maps_path, mapName + ".txt");
            Files.write(mapFile.toPath(), (Iterable<String>) lines::iterator);
        } catch (Exception e) {
            logger.error("writeMap error", e);
        }
    }

    private long readFromStream(Stream<String> in) {
        return in.map(l -> l.split(":")).filter(l -> l.length >= 2).peek(splitLine -> {
            if (splitLine[0].startsWith(MagicStrings.remote_map_key)) {
                if (splitLine.length >= 3) {
                    host = splitLine[1];
                    botid = splitLine[2];
                    isExternal = true;
                    logger.info("Created external map at {} {}", host, botid);
                }
            } else {
                String key = splitLine[0].toUpperCase();
                String value = splitLine[1];
                // assume domain element is already normalized for speedier load
                //key = bot.preProcessor.normalize(key).trim();
                put(key, value);
            }
        }).count();
    }

    /**
     * read an AIML map for a bot
     *
     * @param bot the bot associated with this map.
     */
    public long readMap(Bot bot) {
        try {
            File file = new File(bot.maps_path, mapName + ".txt");
            logger.debug("Reading AIML Map {}", file);
            if (file.exists()) {
                return readFromStream(Files.lines(file.toPath()));
            } else {
                logger.info("{} not found", file);
            }
        } catch (Exception e) {
            logger.error("readMap error", e);
        }
        return 0;

    }

    @Override
    public String toString() {
        return valueMap.toString();
    }
}
