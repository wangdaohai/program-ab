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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Bot Properties
 */

public class Properties {

    private static final Logger logger = LoggerFactory.getLogger(Properties.class);

    private final Map<String, String> properties = new HashMap<>();

    /**
     * get the value of a bot property.
     *
     * @param key property name
     * @return property value or a string indicating the property is undefined
     */
    public String get(String key) {
        String result = properties.get(key);
        return result == null ? AIMLDefault.default_property : result;
    }

    /**
     * Read bot properties from a file.
     *
     * @param path file containing bot properties
     */
    public void getProperties(Path path) {
        logger.debug("Get Properties: {}", path);
        try {
            if (!path.toFile().exists()) {
                logger.warn("{} does not exist", path);
                return;
            }
            Files.lines(path).filter(l -> l.contains(":")).forEach(strLine -> {
                String property = strLine.substring(0, strLine.indexOf(':'));
                String value = strLine.substring(strLine.indexOf(':') + 1);
                properties.put(property, value);
            });
        } catch (Exception e) {
            logger.error("getProperties error", e);
        }
    }
}
