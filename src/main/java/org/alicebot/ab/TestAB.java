package org.alicebot.ab;

import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * @since 5/13/2014.
 */
public final class TestAB {

    private static final Logger logger = LoggerFactory.getLogger(TestAB.class);

    private TestAB() {}

    public static String sample_file = "sample.random.txt";

    public static void testChat(Bot bot, boolean doWrites) {
        Chat chatSession = new Chat(bot, doWrites);
        bot.brain.nodeStats();
        while (true) {
            String textLine = IOUtils.readInputTextLine("Human");
            if (textLine == null || textLine.length() < 1) {
                textLine = MagicStrings.null_input;
            }
            if (textLine.equals("q")) {
                System.exit(0);
            } else if (textLine.equals("wq")) {
                bot.writeQuit();
                System.exit(0);
            } else if (textLine.equals("sc")) {
                sraixCache("c:/ab/data/sraixdata6.txt", chatSession);
            } else if (textLine.equals("iqtest")) {
                ChatTest ct = new ChatTest(bot);
                try {
                    ct.testMultisentenceRespond();
                } catch (Exception ex) {
                    logger.error("testChat error", ex);
                }
            } else if (textLine.equals("ab")) {
                testAB(bot, sample_file);
            } else {
                String request = textLine;
                logger.debug("STATE={}:THAT={}:TOPIC={}",
                    request, chatSession.thatHistory.get(0).get(0), chatSession.predicates.get("topic"));
                String response = chatSession.multisentenceRespond(request);
                while (response.contains("&lt;")) { response = response.replace("&lt;", "<"); }
                while (response.contains("&gt;")) { response = response.replace("&gt;", ">"); }
                IOUtils.writeOutputTextLine("Robot", response);
                //System.out.println("Learn graph:");
                //bot.learnGraph.printgraph();
            }
        }
    }

    public static void testBotChat() {
        Bot bot = new Bot("alice");
        logger.info("{} brain upgrades", bot.brain.upgradeCnt);

        //bot.brain.printgraph();
        Chat chatSession = new Chat(bot);
        String request = "Hello.  How are you?  What is your name?  Tell me about yourself.";
        String response = chatSession.multisentenceRespond(request);
        logger.info("Human: {}", request);
        logger.info("Robot: {}", response);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void runTests(Bot bot) {
        MagicBooleans.qa_test_mode = true;
        Chat chatSession = new Chat(bot, false);
        //        bot.preProcessor.normalizeFile("c:/ab/bots/super/aiml/thats.txt", "c:/ab/bots/super/aiml/normalthats.txt");
        bot.brain.nodeStats();
        IOUtils testInput = new IOUtils(MagicStrings.root_path + "/data/lognormal-500.txt", "read");
        //IOUtils testInput = new IOUtils(MagicStrings.root_path + "/data/callmom-inputs.txt", "read");
        IOUtils testOutput = new IOUtils(MagicStrings.root_path + "/data/lognormal-500-out.txt", "write");
        //IOUtils testOutput = new IOUtils(MagicStrings.root_path + "/data/callmom-outputs.txt", "write");
        String textLine = testInput.readLine();
        int i = 1;
        System.out.print(0);
        while (textLine != null) {
            if (textLine == null || textLine.length() < 1) { textLine = MagicStrings.null_input; }
            if (textLine.equals("q")) {
                System.exit(0);
            } else if (textLine.equals("wq")) {
                bot.writeQuit();
                System.exit(0);
            } else if (textLine.equals("ab")) {
                testAB(bot, sample_file);
            } else if (textLine.equals(MagicStrings.null_input)) {
                testOutput.writeLine("");
            } else if (textLine.startsWith("#")) { testOutput.writeLine(textLine); } else {
                String request = textLine;
                logger.debug("STATE={}:THAT={}:TOPIC={}",
                    request, chatSession.thatHistory.get(0).get(0), chatSession.predicates.get("topic"));
                String response = chatSession.multisentenceRespond(request);
                while (response.contains("&lt;")) { response = response.replace("&lt;", "<"); }
                while (response.contains("&gt;")) { response = response.replace("&gt;", ">"); }
                testOutput.writeLine("Robot: " + response);
            }
            textLine = testInput.readLine();

            System.out.print(".");
            if (i % 10 == 0) { System.out.print(" "); }
            if (i % 100 == 0) {
                System.out.println("");
                System.out.print(i + " ");
            }
            i++;
        }
        testInput.close();
        testOutput.close();
        System.out.println("");
    }

    public static void testAB(Bot bot, String sampleFile) {
        LogUtil.activateDebug(true);
        AB ab = new AB(bot, sampleFile);
        ab.ab();
        logger.info("Begin Pattern Suggestor Terminal Interaction");
        ab.terminalInteraction();
    }

    public static void testShortCuts() {
        //testChat(new Bot("alice"));
        //Graphmaster.enableShortCuts = false;
        //Bot bot = new Bot("alice");
        //bot.brain.printgraph();
        //bot.brain.nodeStats();
        //Graphmaster.enableShortCuts = true;
        //bot = new Bot("alice");
        //bot.brain.printgraph();
        //bot.brain.nodeStats();
    }

    public static void sraixCache(String filename, Chat chatSession) {
        MagicBooleans.cache_sraix = true;
        try {
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            int count = 0;
            int limit = 650000;
            while ((strLine = br.readLine()) != null && count++ < limit) {
                logger.info("Human: {}", strLine);

                String response = chatSession.multisentenceRespond(strLine);
                logger.info("Robot: {}", response);
            }
        } catch (Exception ex) {
            logger.error("sraixCache error", ex);
        }
    }

}
