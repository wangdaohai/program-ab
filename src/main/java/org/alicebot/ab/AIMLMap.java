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

import java.io.*;
import java.util.HashMap;

/**
 * implements AIML Map
 * <p>
 * A map is a function from one string set to another.
 * Elements of the domain are called keys and elements of the range are called values.
 */
public class AIMLMap extends HashMap<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(AIMLMap.class);

    public String mapName;
    String host; // for external maps
    String botid; // for external maps
    boolean isExternal = false;
    Inflector inflector = new Inflector();
    Bot bot;

    /**
     * constructor to create a new AIML Map
     *
     * @param name the name of the map
     */
    public AIMLMap(String name, Bot bot) {
        this.bot = bot;
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
        } else if (mapName.equals("singular")) {
            return inflector.singularize(key).toLowerCase();
        } else if (mapName.equals("plural")) {
            return inflector.pluralize(key).toLowerCase();
        } else if (isExternal && MagicBooleans.enable_external_sets) {
            //String[] split = key.split(" ");
            String query = mapName.toUpperCase() + " " + key;
            String response = Sraix.sraix(null, query, MagicStrings.default_map, null, host, botid, null, "0");
            logger.info("External {}({})={}", mapName, key, response);
            value = response;
        } else {
            value = super.get(key);
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
    @Override
    public String put(String key, String value) {
        //System.out.println("AIMLMap put "+key+"="+value);
        return super.put(key, value);
    }

    public void writeAIMLMap() {
        logger.info("Writing AIML Map {}", mapName);
        try {
            // Create file
            FileWriter fstream = new FileWriter(bot.maps_path + "/" + mapName + ".txt");
            BufferedWriter out = new BufferedWriter(fstream);
            for (String p : this.keySet()) {
                p = p.trim();
                //System.out.println(p+"-->"+this.get(p));
                out.write(p + ":" + this.get(p).trim());
                out.newLine();
            }
            //Close the output stream
            out.close();
        } catch (Exception e) {
            logger.error("writeAIMLMap error", e);
        }
    }

    public int readAIMLMapFromInputStream(InputStream in, Bot bot) {
        int cnt = 0;
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        //Read File Line By Line
        try {
            String strLine;
            while ((strLine = br.readLine()) != null && !strLine.isEmpty()) {
                String[] splitLine = strLine.split(":");
                //System.out.println("AIMLMap line="+strLine);
                if (splitLine.length >= 2) {
                    cnt++;
                    if (strLine.startsWith(MagicStrings.remote_map_key)) {
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
                }
            }
        } catch (Exception ex) {
            logger.error("readAIMLMapFromInputStream error", ex);
        }
        return cnt;
    }

    /**
     * read an AIML map for a bot
     *
     * @param bot the bot associated with this map.
     */
    public int readAIMLMap(Bot bot) {
        try {
            // Open the file that is the first
            // command line parameter
            File file = new File(bot.maps_path, mapName + ".txt");
            logger.debug("Reading AIML Map {}", file);
            if (file.exists()) {
                try (FileInputStream fstream = new FileInputStream(file)) {
                    return readAIMLMapFromInputStream(fstream, bot);
                }
            } else {
                logger.info("{} not found", file);
            }
        } catch (Exception e) {
            logger.error("readAIMLMap error", e);
        }
        return 0;

    }

}
