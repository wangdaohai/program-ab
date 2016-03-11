
/* Program AB Reference AIML 2.1 implementation
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

import org.alicebot.ab.AB;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Category;
import org.alicebot.ab.Chat;
import org.alicebot.ab.ChatTest;
import org.alicebot.ab.Graphmaster;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicNumbers;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.Nodemapper;
import org.alicebot.ab.TestAB;
import org.alicebot.ab.Verbs;
import org.alicebot.ab.aiml.AIMLProcessor;
import org.alicebot.ab.aiml.PCAIMLProcessorExtension;
import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private Main() {}

    public static void main(String[] args) throws IOException {

        IOUtils.setRootPath();

        AIMLProcessor.registerExtensions(PCAIMLProcessorExtension.values());
        mainFunction(args);
    }

    private static void mainFunction(String[] args) throws IOException {
        String botName = "alice2";
        MagicBooleans.jp_tokenize = false;
        LogUtil.activateDebug(true);
        String action = "chat";
        logger.info(MagicStrings.program_name_version);
        for (String s : args) {
            //System.out.println(s);
            String[] splitArg = s.split("=");
            if (splitArg.length >= 2) {
                String option = splitArg[0];
                String value = splitArg[1];
                logger.trace("{}='{}'", option, value);
                if ("bot".equals(option)) { botName = value; }
                if ("action".equals(option)) { action = value; }
                if ("trace".equals(option)) {
                    LogUtil.activateDebug("true".equals(value));
                }
                if ("morph".equals(option)) {
                    MagicBooleans.jp_tokenize = "true".equals(value);
                }
            }
        }
        logger.debug("Working Directory = {}", IOUtils.rootPath);
        Graphmaster.enableShortCuts = true;
        //Timer timer = new Timer();
        Bot bot = new Bot(botName, IOUtils.rootPath, action); //
        //EnglishNumberToWords.makeSetMap(bot);
        //getGloss(bot, "c:/ab/data/wn30-lfs/wne-2006-12-06.xml");
        if (MagicBooleans.make_verbs_sets_maps) { Verbs.makeVerbSetsMaps(bot); }
        //bot.preProcessor.normalizeFile("c:/ab/data/log2.txt", "c:/ab/data/log2normal.txt");
        //System.exit(0);
        if (bot.brain.getCategories().size() < MagicNumbers.brain_print_size) { bot.brain.printgraph(); }
        logger.debug("Action = '{}'", action);
        if ("chat".equals(action) || "chat-app".equals(action)) {
            boolean doWrites = !"chat-app".equals(action);
            TestAB.testChat(bot, doWrites);
        }
        //else if (action.equals("test")) testSuite(bot, MagicStrings.rootPath+"/data/find.txt");
        else if ("ab".equals(action)) {
            TestAB.testAB(bot, TestAB.sample_file);
        } else if ("aiml2csv".equals(action) || "csv2aiml".equals(action)) {
            convert(bot, action);
        } else if ("abwq".equals(action)) {
            AB ab = new AB(bot, TestAB.sample_file);
            ab.abwq();
        } else if ("test".equals(action)) {
            TestAB.runTests(bot);
        } else if ("shadow".equals(action)) {
            LogUtil.activateDebug(false);
            bot.shadowChecker();
        } else if ("iqtest".equals(action)) {
            ChatTest ct = new ChatTest(bot);
            try {
                ct.testMultisentenceRespond();
            } catch (Exception ex) {
                logger.error("testMultisentenceRespond", ex);
            }
        } else {
            logger.warn("Unrecognized action {}", action);
        }
    }

    private static void convert(Bot bot, String action) {
        if ("aiml2csv".equals(action)) {
            bot.writeAIMLIFFiles();
        } else if ("csv2aiml".equals(action)) {
            bot.writeAIMLFiles();
        }
    }

    public static void getGloss(Bot bot, String filename) {
        logger.info("getGloss");
        try {
            // Open the file that is the first
            // command line parameter
            File file = new File(filename);
            if (file.exists()) {
                FileInputStream fstream = new FileInputStream(filename);
                // Get the object
                getGlossFromInputStream(bot, fstream);
                fstream.close();
            }
        } catch (Exception e) {//Catch exception if any
            logger.error("getGloss error", e);
        }
    }

    private static void getGlossFromInputStream(Bot bot, InputStream in) {
        logger.info("getGlossFromInputStream");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        Map<String, String> def = new HashMap<>();
        try {
            //Read File Line By Line
            String word = null;
            String gloss = null;
            String strLine;
            while ((strLine = br.readLine()) != null) {

                if (strLine.contains("<entry word")) {
                    int start = strLine.indexOf("<entry word=\"") + "<entry word=\"".length();
                    //int end = strLine.indexOf(" status=");
                    int end = strLine.indexOf('#');

                    word = strLine.substring(start, end);
                    word = word.replaceAll("_", " ");
                    logger.info(word);

                } else if (strLine.contains("<gloss>")) {
                    gloss = strLine.replaceAll("<gloss>", "");
                    gloss = gloss.replaceAll("</gloss>", "");
                    gloss = gloss.trim();
                    logger.info(gloss);

                }

                if (word != null && gloss != null) {
                    word = word.toLowerCase().trim();
                    if (gloss.length() > 2) {
                        gloss = gloss.substring(0, 1).toUpperCase() + gloss.substring(1, gloss.length());
                    }
                    String definition;
                    if (def.keySet().contains(word)) {
                        definition = def.get(word) + "; " + gloss;
                    } else {
                        definition = gloss;
                    }
                    def.put(word, definition);
                    word = null;
                    gloss = null;
                }
            }
            int filecnt = 0;
            Category d = new Category(0, "WNDEF *", "*", "*", "unknown", "wndefs" + filecnt + ".aiml");
            bot.brain.addCategory(d);
            int cnt = 0;
            for (String x : def.keySet()) {
                word = x;
                gloss = def.get(word) + ".";
                cnt++;
                if (cnt % 5000 == 0) { filecnt++; }

                Category c = new Category(0, "WNDEF " + word, "*", "*", gloss, "wndefs" + filecnt + ".aiml");
                logger.info("{} {} {}:{}:{}", cnt, filecnt, c.inputThatTopic(), c.getTemplate(), c.getFilename());
                Nodemapper node;
                if ((node = bot.brain.findNode(c)) != null) {
                    node.category.setTemplate(node.category.getTemplate() + "," + gloss);
                }
                bot.brain.addCategory(c);

            }
        } catch (Exception ex) {
            logger.error("getGlossFromInputStream error", ex);
        }
    }

    public static void sraixCache(String filename, Chat chatSession) {
        try {
            Files.lines(Paths.get(filename)).limit(1000).forEach(strLine -> {
                logger.info("Human: {}", strLine);

                String response = chatSession.multisentenceRespond(strLine);
                logger.info("Robot: {}", response);
            });
        } catch (Exception ex) {
            logger.error("sraixCache error", ex);
        }
    }

}
