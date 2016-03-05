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

import org.alicebot.ab.utils.JapaneseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage client predicates
 */
public class Predicates {

    private static final Logger logger = LoggerFactory.getLogger(Predicates.class);

    private final Map<String, String> valueMap = new HashMap<>();

    /**
     * save a predicate value
     *
     * @param key   predicate name
     * @param value predicate value
     * @return predicate value
     */
    public String put(String key, String value) {
        //MagicBooleans.trace("predicates.put(key: " + key + ", value: " + value + ")");
        if (MagicBooleans.jp_tokenize) {
            if ("topic".equals(key)) { value = JapaneseUtils.tokenizeSentence(value); }
        }
        if ("topic".equals(key) && value.isEmpty()) { value = MagicStrings.default_get; }
        if (value.equals(MagicStrings.too_much_recursion)) { value = MagicStrings.default_list_item; }
        return valueMap.put(key, value);
    }

    /**
     * get a predicate value
     *
     * @param key predicate name
     * @return predicate value
     */
    public String get(String key) {
        //MagicBooleans.trace("predicates.get(key: " + key + ")");
        String result = valueMap.get(key);
        if (result == null) { result = MagicStrings.default_get; }
        //MagicBooleans.trace("in predicates.get, returning: " + result);
        return result;
    }

    public boolean contains(String key) {
        return valueMap.containsKey(key);
    }

    /**
     * Read predicate default values from an input stream
     *
     * @param in input stream
     */
    public void getPredicateDefaultsFromInputStream(InputStream in) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        try {
            //Read File Line By Line
            String strLine;
            while ((strLine = br.readLine()) != null) {
                if (strLine.contains(":")) {
                    String property = strLine.substring(0, strLine.indexOf(':'));
                    String value = strLine.substring(strLine.indexOf(':') + 1);
                    put(property, value);
                }
            }
        } catch (Exception ex) {
            logger.error("getPredicateDefaultsFromInputStream error", ex);
        }
    }

    /**
     * read predicate defaults from a file
     *
     * @param filename name of file
     */
    public void getPredicateDefaults(String filename) {
        try {
            // Open the file that is the first
            // command line parameter
            File file = new File(filename);
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object
                getPredicateDefaultsFromInputStream(fstream);
                fstream.close();
            }
        } catch (Exception e) {
            logger.error("getPredicateDefaults error", e);
        }
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }
}


