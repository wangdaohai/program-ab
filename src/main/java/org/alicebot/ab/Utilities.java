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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Utilities {

    private static final Logger logger = LoggerFactory.getLogger(Utilities.class);

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

    public static Stream<String> lines(File file) {
        try {
            if (file.exists()) {
                return Files.lines(file.toPath())
                    .filter(line -> !line.startsWith(MagicStrings.text_comment_mark));
            }
        } catch (Exception e) {
            logger.error("lines error", e);
        }
        return Stream.empty();
    }

    public static String getFile(File file) {
        return lines(file).collect(Collectors.joining("\n"));
    }

    public static String getCopyright(Bot bot, String AIMLFilename) {
        String copyright = "";
        String year = CalendarUtils.year();
        String date = CalendarUtils.date();
        try {
            copyright = lines(new File(bot.config_path, "copyright.txt"))
                .map(line -> "<!-- " + line + " -->")
                .collect(Collectors.joining("\n", "", "\n"))
                .replace("[url]", bot.properties.get("url"))
                .replace("[date]", date)
                .replace("[YYYY]", year)
                .replace("[version]", bot.properties.get("version"))
                .replace("[botname]", bot.name.toUpperCase())
                .replace("[filename]", AIMLFilename)
                .replace("[botmaster]", bot.properties.get("botmaster"))
                .replace("[organization]", bot.properties.get("organization"));
        } catch (Exception e) {
            logger.error("getCopyright error", e);
        }
        return copyright + "<!--  -->\n";
    }

    public static String getPannousAPIKey(Bot bot) {
        String apiKey = getFile(new File(bot.config_path, "pannous-apikey.txt"));
        if (apiKey.isEmpty()) { apiKey = MagicStrings.pannous_api_key; }
        return apiKey;
    }

    public static String getPannousLogin(Bot bot) {
        String login = getFile(new File(bot.config_path, "pannous-login.txt"));
        if (login.isEmpty()) { login = MagicStrings.pannous_login; }
        return login;
    }

    private static final Collection<Character.UnicodeBlock> CJK_CHARS = Arrays.asList(
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS,
        Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT,
        Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
        Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS
    );

    /**
     * Returns if a character is one of Chinese-Japanese-Korean characters.
     *
     * @param c the character to be tested
     * @return true if CJK, false otherwise
     */
    public static boolean isCharCJK(final char c) {
        return CJK_CHARS.contains(Character.UnicodeBlock.of(c));
    }

}
