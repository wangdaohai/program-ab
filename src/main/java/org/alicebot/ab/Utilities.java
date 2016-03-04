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

import org.alicebot.ab.utils.CalendarUtils;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class Utilities {

    private Utilities() {}

    /**
     * Excel sometimes adds mysterious formatting to CSV files.
     * This function tries to clean it up.
     *
     * @param line line from AIMLIF file
     * @return reformatted line
     */
    public static String fixCSV(String line) {
        while (line.endsWith(";")) { line = line.substring(0, line.length() - 1); }
        if (line.startsWith("\"")) { line = line.substring(1, line.length()); }
        if (line.endsWith("\"")) { line = line.substring(0, line.length() - 1); }
        line = line.replaceAll("\"\"", "\"");
        return line;
    }

    public static String tagTrim(String xmlExpression, String tagName) {
        String stag = "<" + tagName + ">";
        String etag = "</" + tagName + ">";
        if (xmlExpression.length() >= (stag + etag).length()) {
            xmlExpression = xmlExpression.substring(stag.length());
            xmlExpression = xmlExpression.substring(0, xmlExpression.length() - etag.length());
        }
        return xmlExpression;
    }

    public static Set<String> stringSet(String... strings) {
        Set<String> set = new HashSet<>();
        Collections.addAll(set, strings);
        return set;
    }

    public static String getFileFromInputStream(InputStream in) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        //Read File Line By Line
        StringBuilder contents = new StringBuilder();
        try {
            String strLine;
            while ((strLine = br.readLine()) != null) {
                if (!strLine.startsWith(MagicStrings.text_comment_mark)) {
                    contents.append(strLine + "\n");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return contents.toString().trim();
    }

    public static String getFile(String filename) {
        String contents = "";
        try {
            File file = new File(filename);
            if (file.exists()) {
                //System.out.println("Found file "+filename);
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object
                contents = getFileFromInputStream(fstream);
                fstream.close();
            }
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        //System.out.println("getFile: "+contents);
        return contents;
    }

    public static String getCopyrightFromInputStream(InputStream in) {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        //Read File Line By Line
        StringBuilder copyright = new StringBuilder();
        try {
            String strLine;
            while ((strLine = br.readLine()) != null) {
                copyright.append((strLine.isEmpty()) ? "\n" : ("<!-- " + strLine + " -->\n"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return copyright.toString();
    }

    public static String getCopyright(Bot bot, String AIMLFilename) {
        String copyright = "";
        String year = CalendarUtils.year();
        String date = CalendarUtils.date();
        try {
            copyright = getFile(bot.config_path + "/copyright.txt");
            String[] splitCopyright = copyright.split("\n");
            copyright = "";
            for (String aSplitCopyright : splitCopyright) {
                copyright += "<!-- " + aSplitCopyright + " -->\n";
            }
            copyright = copyright.replace("[url]", bot.properties.get("url"));
            copyright = copyright.replace("[date]", date);
            copyright = copyright.replace("[YYYY]", year);
            copyright = copyright.replace("[version]", bot.properties.get("version"));
            copyright = copyright.replace("[botname]", bot.name.toUpperCase());
            copyright = copyright.replace("[filename]", AIMLFilename);
            copyright = copyright.replace("[botmaster]", bot.properties.get("botmaster"));
            copyright = copyright.replace("[organization]", bot.properties.get("organization"));
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        copyright += "<!--  -->\n";
        //System.out.println("Copyright: "+copyright);
        return copyright;
    }

    public static String getPannousAPIKey(Bot bot) {
        String apiKey = getFile(bot.config_path + "/pannous-apikey.txt");
        if (apiKey.isEmpty()) { apiKey = MagicStrings.pannous_api_key; }
        return apiKey;
    }

    public static String getPannousLogin(Bot bot) {
        String login = getFile(bot.config_path + "/pannous-login.txt");
        if (login.isEmpty()) { login = MagicStrings.pannous_login; }
        return login;
    }

    /**
     * Returns if a character is one of Chinese-Japanese-Korean characters.
     *
     * @param c the character to be tested
     * @return true if CJK, false otherwise
     */
    public static boolean isCharCJK(final char c) {
        return (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
            || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A)
            || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B)
            || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS)
            || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS)
            || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT)
            || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION)
            || (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS);
    }

}
