package org.alicebot.ab;

import org.alicebot.ab.aiml.AIMLDefault;
import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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
                textLine = AIMLDefault.null_input;
            }
            if ("q".equals(textLine)) {
                System.exit(0);
            } else if ("wq".equals(textLine)) {
                bot.writeQuit();
                System.exit(0);
            } else if ("sc".equals(textLine)) {
                sraixCache(IOUtils.rootPath.resolve("data/sraixdata6.txt"), chatSession);
            } else if ("iqtest".equals(textLine)) {
                ChatTest ct = new ChatTest(bot);
                try {
                    ct.testMultisentenceRespond();
                } catch (Exception ex) {
                    logger.error("testChat error", ex);
                }
            } else if ("ab".equals(textLine)) {
                testAB(bot, sample_file);
            } else {
                logger.debug("STATE={}:THAT={}:TOPIC={}",
                    textLine, chatSession.thatHistory.get(0).get(0), chatSession.predicates.get("topic"));
                String response = chatSession.multisentenceRespond(textLine);
                while (response.contains("&lt;")) { response = response.replace("&lt;", "<"); }
                while (response.contains("&gt;")) { response = response.replace("&gt;", ">"); }
                //noinspection UseOfSystemOutOrSystemErr
                System.out.println("Robot: " + response);
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
    public static void runTests(Bot bot) throws IOException {
        MagicBooleans.qa_test_mode = true;
        Chat chatSession = new Chat(bot, false);
        //        bot.preProcessor.normalizeFile("c:/ab/bots/super/aiml/thats.txt", "c:/ab/bots/super/aiml/normalthats.txt");
        bot.brain.nodeStats();
        BufferedReader testInput = Files.newBufferedReader(IOUtils.rootPath.resolve("data/lognormal-500.txt"));
        //IOUtils testInput = new IOUtils(MagicStrings.rootPath + "/data/callmom-inputs.txt", "read");
        BufferedWriter testOutput = Files.newBufferedWriter(IOUtils.rootPath.resolve("data/lognormal-500-out.txt"),
            StandardOpenOption.TRUNCATE_EXISTING);
        //IOUtils testOutput = new IOUtils(MagicStrings.rootPath + "/data/callmom-outputs.txt", "write");
        String textLine = testInput.readLine();
        int i = 1;
        System.out.print(0);
        while (textLine != null) {
            if (textLine == null || textLine.length() < 1) { textLine = AIMLDefault.null_input; }
            if ("q".equals(textLine)) {
                System.exit(0);
            } else if ("wq".equals(textLine)) {
                bot.writeQuit();
                System.exit(0);
            } else if ("ab".equals(textLine)) {
                testAB(bot, sample_file);
            } else if (textLine.equals(AIMLDefault.null_input)) {
                testOutput.newLine();
            } else if (textLine.startsWith("#")) {
                testOutput.write(textLine);
                testOutput.newLine();
            } else {
                logger.debug("STATE={}:THAT={}:TOPIC={}",
                    textLine, chatSession.thatHistory.get(0).get(0), chatSession.predicates.get("topic"));
                String response = chatSession.multisentenceRespond(textLine);
                while (response.contains("&lt;")) { response = response.replace("&lt;", "<"); }
                while (response.contains("&gt;")) { response = response.replace("&gt;", ">"); }
                testOutput.write("Robot: " + response);
                testOutput.newLine();
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

    public static void sraixCache(Path path, Chat chatSession) {
        MagicBooleans.cache_sraix = true;
        try {
            Files.lines(path).limit(650000).forEach(strLine -> {
                logger.info("Human: {}", strLine);
                String response = chatSession.multisentenceRespond(strLine);
                logger.info("Robot: {}", response);
            });
        } catch (Exception ex) {
            logger.error("sraixCache error", ex);
        }
    }

}
