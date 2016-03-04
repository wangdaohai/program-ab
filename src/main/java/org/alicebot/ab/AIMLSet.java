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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * implements AIML Sets
 */
public class AIMLSet extends HashSet<String> {

    private static final Logger logger = LoggerFactory.getLogger(AIMLSet.class);

    public String setName;
    int maxLength = 1; // there are no empty sets
    String host; // for external sets
    String botid; // for external sets
    boolean isExternal = false;
    Bot bot;
    private final Set<String> inCache = new HashSet<>();
    private final Set<String> outCache = new HashSet<>();

    /**
     * constructor
     *
     * @param name name of set
     */
    public AIMLSet(String name, Bot bot) {
        this.bot = bot;
        this.setName = name.toLowerCase();
        if (setName.equals(MagicStrings.natural_number_set_name)) { maxLength = 1; }
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
            return super.contains(s);
        }
    }

    public void writeAIMLSet() {
        logger.info("Writing AIML Set {}", setName);
        try {
            // Create file
            FileWriter fstream = new FileWriter(bot.sets_path + "/" + setName + ".txt");
            BufferedWriter out = new BufferedWriter(fstream);
            for (String p : this) {

                out.write(p.trim());
                out.newLine();
            }
            //Close the output stream
            out.close();
        } catch (Exception e) {
            logger.error("writeAIMLSet error", e);
        }
    }

    public int readAIMLSetFromInputStream(InputStream in, Bot bot) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        int cnt = 0;
        //Read File Line By Line
        try {
            String strLine;
            while ((strLine = br.readLine()) != null && !strLine.isEmpty()) {
                cnt++;
                //strLine = bot.preProcessor.normalize(strLine).toUpperCase();
                // assume the set is pre-normalized for faster loading
                if (strLine.startsWith("external")) {
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
                    //System.out.println("readAIMLSetFromInputStream "+strLine);
                    add(strLine.trim());
                }
                /*Category c = new Category(0, "ISA"+setName.toUpperCase()+" "+strLine.toUpperCase(), "*", "*", "true", MagicStrings.null_aiml_file);
                bot.brain.addCategory(c);*/
            }
        } catch (Exception ex) {
            logger.error("readAIMLSetFromInputStream error", ex);
        }
        return cnt;
    }

    public int readAIMLSet(Bot bot) {
        try {
            // Open the file that is the first
            // command line parameter
            File file = new File(bot.sets_path + "/" + setName + ".txt");
            logger.debug("Reading AIML Set {}", file);
            if (file.exists()) {
                try (FileInputStream fStream = new FileInputStream(file)) {
                    return readAIMLSetFromInputStream(fStream, bot);
                }
            } else {
                logger.warn("{} not found", file);
            }
        } catch (Exception e) {
            logger.error("readAIMLSet error", e);
        }
        return 0;
    }

}
