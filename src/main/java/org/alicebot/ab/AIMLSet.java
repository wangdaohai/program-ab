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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractCollection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * implements AIML Sets
 */
public class AIMLSet extends AbstractCollection<String> {

    private static final Logger logger = LoggerFactory.getLogger(AIMLSet.class);

    private final Set<String> valueSet = new HashSet<>();
    public String setName;
    int maxLength = 1; // there are no empty sets
    String host; // for external sets
    String botid; // for external sets
    boolean isExternal = false;
    private final Set<String> inCache = new HashSet<>();
    private final Set<String> outCache = new HashSet<>();

    /**
     * constructor
     *
     * @param name name of set
     */
    public AIMLSet(String name) {
        this.setName = name.toLowerCase();
        if (setName.equals(MagicStrings.natural_number_set_name)) { maxLength = 1; }
    }

    @Override
    public Iterator<String> iterator() {
        return valueSet.iterator();
    }

    @Override
    public int size() {
        return valueSet.size();
    }

    @Override
    public boolean add(String s) {
        return valueSet.add(s);
    }

    public boolean contains(String s) {
        //if (isExternal)  System.out.println("External "+setName+" contains "+s+"?");
        //else  System.out.println("Internal "+setName+" contains "+s+"?");
        if (isExternal && MagicBooleans.enable_external_sets) {
            if (inCache.contains(s)) { return true; }
            if (outCache.contains(s)) { return false; }
            String[] split = s.split(" ");
            if (split.length > maxLength) { return false; }
            String query = MagicStrings.set_member_string + setName.toUpperCase() + " " + s;
            String response = Sraix.sraix(null, query, "false", null, host, botid, null, "0");
            //System.out.println("External "+setName+" contains "+s+"? "+response);
            if ("true".equals(response)) {
                inCache.add(s);
                return true;
            } else {
                outCache.add(s);
                return false;
            }
        } else if (setName.equals(MagicStrings.natural_number_set_name)) {
            Pattern numberPattern = Pattern.compile("[0-9]+");
            Matcher numberMatcher = numberPattern.matcher(s);
            return numberMatcher.matches();
        } else {
            return valueSet.contains(s);
        }
    }

    public void writeSet(Bot bot) {
        logger.info("Writing AIML Set {}", setName);
        try {
            Stream<String> lines = valueSet.stream().map(String::trim);
            Path setFile = bot.setsPath.resolve(setName + ".txt");
            Files.write(setFile, (Iterable<String>) lines::iterator);
        } catch (Exception e) {
            logger.error("writeSet error", e);
        }
    }

    public long readFromStream(Stream<String> in) {
        return in.peek(strLine -> {
            //strLine = bot.preProcessor.normalize(strLine).toUpperCase();
            // assume the set is pre-normalized for faster loading
            if (strLine.startsWith(MagicStrings.remote_set_key)) {
                String[] splitLine = strLine.split(":");
                if (splitLine.length >= 4) {
                    host = splitLine[1];
                    botid = splitLine[2];
                    maxLength = Integer.parseInt(splitLine[3]);
                    isExternal = true;
                    logger.info("Created external set at {} {}", host, botid);
                }
            } else {
                strLine = strLine.toUpperCase().trim();
                String[] splitLine = strLine.split(" ");
                int length = splitLine.length;
                if (length > maxLength) { maxLength = length; }
                //logger.debug("readFromStream {}", strLine);
                valueSet.add(strLine.trim());
            }
            /*Category c = new Category(0, "ISA"+setName.toUpperCase()+" "+strLine.toUpperCase(), "*", "*", "true", MagicStrings.null_aiml_file);
            bot.brain.addCategory(c);*/
        }).count();
    }

    public long readSet(Bot bot) {
        Path path = bot.setsPath.resolve(setName + ".txt");
        try {
            logger.debug("Reading AIML Set {}", path);
            if (path.toFile().exists()) {
                return readFromStream(Files.lines(path));
            } else {
                logger.warn("{} not found", path);
            }
        } catch (Exception e) {
            logger.error("readSet error for file {}", path, e);
        }
        return 0;
    }

}
