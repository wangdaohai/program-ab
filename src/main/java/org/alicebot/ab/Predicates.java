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

import org.alicebot.ab.aiml.AIMLDefault;
import org.alicebot.ab.utils.JapaneseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
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
        if ("topic".equals(key) && value.isEmpty()) { value = AIMLDefault.default_get; }
        if (value.equals(AIMLDefault.too_much_recursion)) { value = AIMLDefault.default_list_item; }
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
        if (result == null) { result = AIMLDefault.default_get; }
        //MagicBooleans.trace("in predicates.get, returning: " + result);
        return result;
    }

    public boolean contains(String key) {
        return valueMap.containsKey(key);
    }

    /**
     * read predicate defaults from a file
     *
     * @param path name of file
     */
    public void getPredicateDefaults(Path path) {
        try {
            if (!path.toFile().exists()) {
                logger.warn("{} does not exist", path);
                return;
            }
            Files.lines(path).filter(l -> l.contains(":")).forEach(strLine -> {
                String property = strLine.substring(0, strLine.indexOf(':'));
                String value = strLine.substring(strLine.indexOf(':') + 1);
                put(property, value);
            });
        } catch (Exception e) {
            logger.error("getPredicateDefaults error", e);
        }
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }
}


