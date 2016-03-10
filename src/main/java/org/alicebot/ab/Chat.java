package org.alicebot.ab;

import org.alicebot.ab.aiml.AIMLDefault;
import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.JapaneseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

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

/**
 * Class encapsulating a chat session between a bot and a client
 */
public class Chat {

    private static final Logger logger = LoggerFactory.getLogger(Chat.class);
    private static final String REPETITION_DETECTED = "REPETITIONDETECTED";

    public Bot bot;
    private boolean doWrites;
    public final String customerId;
    public History<History<String>> thatHistory = History.ofHistory("that");
    public History<String> requestHistory = History.ofString("request");
    public History<String> responseHistory = History.ofString("response");
    // public History<String> repetitionHistory = History.ofString("repetition");
    public History<String> inputHistory = History.ofString("input");
    public Predicates predicates = new Predicates();
    public static String matchTrace = "";
    public static boolean locationKnown = false;
    public static String longitude;
    public static String latitude;
    public TripleStore tripleStore = new TripleStore("anon", this);

    /**
     * Constructor  (defualt customer ID)
     *
     * @param bot the bot to chat with
     */
    public Chat(Bot bot) {
        this(bot, true, "0");
    }

    public Chat(Bot bot, boolean doWrites) {
        this(bot, doWrites, "0");
    }

    /**
     * Constructor
     *
     * @param bot        bot to chat with
     * @param customerId unique customer identifier
     */
    private Chat(Bot bot, boolean doWrites, String customerId) {
        this.customerId = customerId;
        this.bot = bot;
        this.doWrites = doWrites;
        History<String> contextThatHistory = History.ofString("unknown");
        contextThatHistory.add(AIMLDefault.default_that);
        thatHistory.add(contextThatHistory);
        addPredicates();
        addTriples();
        predicates.put("topic", AIMLDefault.default_topic);
        predicates.put("jsenabled", AIMLDefault.js_enabled);
        logger.debug("Chat Session Created for bot {}", bot.name);
    }

    /**
     * Load all predicate defaults
     */
    private void addPredicates() {
        try {
            predicates.getPredicateDefaults(bot.configPath.resolve("predicates.txt"));
        } catch (Exception ex) {
            logger.error("addPredicates error", ex);
        }
    }

    /**
     * Load Triple Store knowledge base
     */
    private void addTriples() {
        Path path = bot.configPath.resolve("triples.txt");
        logger.debug("Loading Triples from {}", path);
        int tripleCnt = 0;
        if (path.toFile().exists()) {
            try {
                tripleCnt = (int) Files.lines(path)
                    .map(l -> l.split(":")).filter(triple -> triple.length >= 3)
                    .peek(triple -> {
                        String subject = triple[0];
                        String predicate = triple[1];
                        String object = triple[2];
                        tripleStore.addTriple(subject, predicate, object);
                        //logger.debug("Added Triple: {} {} {}", subject, predicate, object);
                    }).count();
            } catch (Exception ex) {
                logger.error("addTriples error", ex);
            }
        }
        logger.debug("Loaded {} triples", tripleCnt);
    }

    /**
     * Chat session terminal interaction
     */
    public void chat() {
        Path logFile = bot.logPath.resolve("log_" + customerId + ".txt");
        try {
            //Construct the bw object
            BufferedWriter bw = Files.newBufferedWriter(logFile);
            String request = "SET PREDICATES";
            multisentenceRespond(request);
            while (!"quit".equals(request)) {
                request = IOUtils.readInputTextLine("Human");
                String response = multisentenceRespond(request);
                //noinspection UseOfSystemOutOrSystemErr
                System.out.println("Robot: " + response);
                bw.write("Human: " + request);
                bw.newLine();
                bw.write("Robot: " + response);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception ex) {
            logger.error("chat error", ex);
        }
    }

    /**
     * Return bot response to a single sentence input given conversation context
     *
     * @param input              client input
     * @param that               bot's last sentence
     * @param topic              current topic
     * @param contextThatHistory history of "that" values for this request/response interaction
     * @return bot's reply
     */
    private String respond(String input, String that, String topic, History<String> contextThatHistory) {
        //MagicBooleans.trace("chat.respond(input: " + input + ", that: " + that + ", topic: " + topic + ", contextThatHistory: " + contextThatHistory + ")");
        boolean repetition = true;
        //inputHistory.printHistory();
        for (int i = 0; i < MagicNumbers.repetition_count; i++) {
            //System.out.println(request.toUpperCase()+"=="+inputHistory.get(i)+"? "+request.toUpperCase().equals(inputHistory.get(i)));
            if (inputHistory.get(i) == null || !input.toUpperCase().equals(inputHistory.get(i).toUpperCase())) {
                repetition = false;
            }
        }
        if (input.equals(AIMLDefault.null_input)) { repetition = false; }
        inputHistory.add(input);
        if (repetition) {input = REPETITION_DETECTED;}

        String response = AIMLProcessor.respond(input, that, topic, this);
        //MagicBooleans.trace("in chat.respond(), response: " + response);
        String normResponse = bot.preProcessor.normalize(response);
        //MagicBooleans.trace("in chat.respond(), normResponse: " + normResponse);
        if (MagicBooleans.jp_tokenize) { normResponse = JapaneseUtils.tokenizeSentence(normResponse); }
        String[] sentences = bot.preProcessor.sentenceSplit(normResponse);
        for (String sentence : sentences) {
            that = sentence;
            //System.out.println("That "+i+" '"+that+"'");
            if (that.trim().isEmpty()) { that = AIMLDefault.default_that; }
            contextThatHistory.add(that);
        }
        return response.trim() + "  ";
    }

    /**
     * Return bot response given an input and a history of "that" for the current conversational interaction
     *
     * @param input              client input
     * @param contextThatHistory history of "that" values for this request/response interaction
     * @return bot's reply
     */
    private String respond(String input, History<String> contextThatHistory) {
        History<String> hist = thatHistory.get(0);
        String that = hist == null ? AIMLDefault.default_that : hist.get(0);
        return respond(input, that, predicates.get("topic"), contextThatHistory);
    }

    /**
     * return a compound response to a multiple-sentence request. "Multiple" means one or more.
     *
     * @param request client's multiple-sentence input
     */
    public String multisentenceRespond(String request) {

        //MagicBooleans.trace("chat.multisentenceRespond(request: " + request + ")");
        String response = "";
        matchTrace = "";
        try {
            String normalized = bot.preProcessor.normalize(request);
            normalized = JapaneseUtils.tokenizeSentence(normalized);
            //MagicBooleans.trace("in chat.multisentenceRespond(), normalized: " + normalized);
            String[] sentences = bot.preProcessor.sentenceSplit(normalized);
            History<String> contextThatHistory = History.ofString("contextThat");
            for (String sentence : sentences) {
                //System.out.println("Human: "+sentences[i]);
                AIMLProcessor.trace_count = 0;
                String reply = respond(sentence, contextThatHistory);
                response += "  " + reply;
                //System.out.println("Robot: "+reply);
            }
            requestHistory.add(request);
            responseHistory.add(response);
            thatHistory.add(contextThatHistory);
            response = response.replaceAll("[\n]+", "\n");
            response = response.trim();
        } catch (Exception ex) {
            logger.error("multisentenceRespond error", ex);
            return StandardResponse.ERROR;
        }

        if (doWrites) {
            bot.writeLearnfIFCategories();
        }
        //MagicBooleans.trace("in chat.multisentenceRespond(), returning: " + response);
        return response;
    }

    public static void setMatchTrace(String newMatchTrace) {
        matchTrace = newMatchTrace;
    }
}
