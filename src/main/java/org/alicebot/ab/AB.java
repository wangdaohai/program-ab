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

import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class AB {

    private static final Logger logger = LoggerFactory.getLogger(AB.class);

    /**
     * Experimental class that analyzes log data and suggests
     * new AIML patterns.
     */
    public boolean shuffle_mode = true;
    public boolean sort_mode = !shuffle_mode;
    public boolean filter_atomic_mode = false;
    public boolean filter_wild_mode = false;
    public boolean offer_alice_responses = true;

    public String logfile = MagicStrings.root_path + "/data/" + MagicStrings.ab_sample_file; //normal.txt";

    public int runCompletedCnt;
    public Bot bot;
    public Bot alice;
    AIMLSet passed;
    AIMLSet testSet;

    public final Graphmaster inputGraph;
    public final Graphmaster patternGraph;
    public final Graphmaster deletedGraph;
    public ArrayList<Category> suggestedCategories;
    public static int limit = 500000;

    public AB(Bot bot, String sampleFile) {
        MagicStrings.ab_sample_file = sampleFile;
        logfile = MagicStrings.root_path + "/data/" + MagicStrings.ab_sample_file;
        logger.info("AB with sample file {}", logfile);
        this.bot = bot;
        this.inputGraph = new Graphmaster(bot, "input");
        this.deletedGraph = new Graphmaster(bot, "deleted");
        this.patternGraph = new Graphmaster(bot, "pattern");
        bot.brain.getCategories().forEach(patternGraph::addCategory);
        this.suggestedCategories = new ArrayList<>();
        passed = new AIMLSet("passed", bot);
        testSet = new AIMLSet("1000", bot);
        readDeletedIFCategories();
    }

    /**
     * Calculates the botmaster's productivity rate in
     * categories/sec when using Pattern Suggestor to create content.
     *
     * @param runCompletedCnt number of categories completed in this run
     * @param timer           tells elapsed time in ms
     */

    public void productivity(int runCompletedCnt, Timer timer) {
        float time = timer.elapsedTimeMins();
        logger.info("Completed {} in {} min. Productivity {} cat/min",
            runCompletedCnt, time, (float) runCompletedCnt / time);
    }

    public void readDeletedIFCategories() {
        bot.readCertainIFCategories(deletedGraph, MagicStrings.deleted_aiml_file);
        logger.debug("--- DELETED CATEGORIES -- read {} deleted categories", deletedGraph.getCategories().size());
    }

    public void writeDeletedIFCategories() {
        logger.info("--- DELETED CATEGORIES -- write");
        bot.writeCertainIFCategories(deletedGraph, MagicStrings.deleted_aiml_file);
        logger.info("--- DELETED CATEGORIES -- write {} deleted categories", deletedGraph.getCategories().size());

    }

    /**
     * saves a new AIML category and increments runCompletedCnt
     *
     * @param pattern  the category's pattern (that and topic = *)
     * @param template the category's template
     * @param filename the filename for the category.
     */
    public void saveCategory(String pattern, String template, String filename) {
        String that = "*";
        String topic = "*";
        Category c = new Category(0, pattern, that, topic, template, filename);

        if (c.validate()) {
            bot.brain.addCategory(c);
            // bot.categories.add(c);
            bot.writeAIMLIFFiles();
            runCompletedCnt++;
        } else {
            logger.info("Invalid Category {}", c.validationMessage);
        }
    }

    /**
     * mark a category as deleted
     *
     * @param c the category
     */
    public void deleteCategory(Category c) {
        c.setFilename(MagicStrings.deleted_aiml_file);
        c.setTemplate(MagicStrings.deleted_template);
        deletedGraph.addCategory(c);
        logger.info("--- bot.writeDeletedIFCategories()");
        writeDeletedIFCategories();
    }

    /**
     * skip a category.  Make the category as "unfinished"
     *
     * @param c the category
     */
    public void skipCategory(Category c) {
       /* c.setFilename(MagicStrings.unfinished_aiml_file);
        c.setTemplate(MagicStrings.unfinished_template);
        bot.unfinishedGraph.addCategory(c);
        System.out.println(bot.unfinishedGraph.getCategories().size() + " unfinished categories");
        bot.writeUnfinishedIFCategories();*/
    }

    public void abwq() {
        Timer timer = new Timer();
        timer.start();
        classifyInputs(logfile);
        logger.info("{} classifying inputs", timer.elapsedTimeSecs());
        bot.writeQuit();
    }

    /**
     * read sample inputs from filename, turn them into Paths, and
     * add them to the graph.
     *
     * @param filename file containing sample inputs
     */
    public void graphInputs(String filename) {
        try {
            // Open the file that is the first
            // command line parameter
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            int count = 0;
            while ((strLine = br.readLine()) != null && count < limit) {
                //strLine = preProcessor.normalize(strLine);
                Category c = new Category(0, strLine, "*", "*", "nothing", MagicStrings.unknown_aiml_file);
                Nodemapper node = inputGraph.findNode(c);
                if (node == null) {
                    inputGraph.addCategory(c);
                    c.incrementActivationCnt();
                } else {
                    node.category.incrementActivationCnt();
                }
                count++;
                //System.out.println("Root branches="+g.root.size());
            }
            //Close the input stream
            br.close();
        } catch (Exception e) {//Catch exception if any
            logger.error("graphInputs error", e);
        }
    }

    static int leafPatternCnt = 0;
    static int starPatternCnt = 0;

    /**
     * find suggested patterns in a graph of inputs
     */
    public void findPatterns() {
        findPatterns(inputGraph.root, "");
        logger.info("{} Leaf Patterns {} Star Patterns", leafPatternCnt, starPatternCnt);
    }

    /**
     * find patterns recursively
     *
     * @param node                    current graph node
     * @param partialPatternThatTopic partial pattern path
     */
    void findPatterns(Nodemapper node, String partialPatternThatTopic) {
        if (NodemapperOperator.isLeaf(node)) {
            //System.out.println("LEAF: "+node.category.getActivationCnt()+". "+partialPatternThatTopic);
            if (node.category.getActivationCnt() > MagicNumbers.node_activation_cnt) {
                //System.out.println("LEAF: "+node.category.getActivationCnt()+". "+partialPatternThatTopic+" "+node.shortCut);    //Start writing to the output stream
                leafPatternCnt++;
                try {
                    String categoryPatternThatTopic;
                    if (node.shortCut) {
                        //System.out.println("Partial patternThatTopic = "+partialPatternThatTopic);
                        categoryPatternThatTopic = partialPatternThatTopic + " <THAT> * <TOPIC> *";
                    } else {
                        categoryPatternThatTopic = partialPatternThatTopic;
                    }
                    Category c = new Category(0, categoryPatternThatTopic, MagicStrings.blank_template, MagicStrings.unknown_aiml_file);
                    //if (brain.existsCategory(c)) System.out.println(c.inputThatTopic()+" Exists");
                    //if (deleted.existsCategory(c)) System.out.println(c.inputThatTopic()+ " Deleted");
                    if (!bot.brain.existsCategory(c) && !deletedGraph.existsCategory(c)/* && !unfinishedGraph.existsCategory(c)*/) {
                        patternGraph.addCategory(c);
                        suggestedCategories.add(c);
                    }
                } catch (Exception e) {
                    logger.error("findPatterns error", e);
                }
            }
        }
        if (NodemapperOperator.size(node) > MagicNumbers.node_size) {
            //System.out.println("STAR: "+NodemapperOperator.size(node)+". "+partialPatternThatTopic+" * <that> * <topic> *");
            starPatternCnt++;
            try {
                Category c = new Category(0, partialPatternThatTopic + " * <THAT> * <TOPIC> *", MagicStrings.blank_template, MagicStrings.unknown_aiml_file);
                //if (brain.existsCategory(c)) System.out.println(c.inputThatTopic()+" Exists");
                //if (deleted.existsCategory(c)) System.out.println(c.inputThatTopic()+ " Deleted");
                if (!bot.brain.existsCategory(c) && !deletedGraph.existsCategory(c)/* && !unfinishedGraph.existsCategory(c)*/) {
                    patternGraph.addCategory(c);
                    suggestedCategories.add(c);
                }
            } catch (Exception e) {
                logger.error("findPatterns error", e);
            }
        }
        for (String key : NodemapperOperator.keySet(node)) {
            Nodemapper value = NodemapperOperator.get(node, key);
            findPatterns(value, partialPatternThatTopic + " " + key);
        }

    }

    /**
     * classify inputs into matching categories
     *
     * @param filename file containing sample normalized inputs
     */

    public void classifyInputs(String filename) {
        try {
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            //Read File Line By Line
            int count = 0;
            while ((strLine = br.readLine()) != null && count < limit) {
                // Print the content on the console
                //System.out.println("Classifying "+strLine);

                if (strLine != null) {
                    if (strLine.startsWith("Human: ")) {
                        strLine = strLine.substring("Human: ".length(), strLine.length());
                    }
                    String[] sentences = bot.preProcessor.sentenceSplit(strLine);
                    for (String sentence : sentences) {
                        if (!sentence.isEmpty()) {
                            Nodemapper match = patternGraph.match(sentence, "unknown", "unknown");

                            if (match == null) {
                                logger.info("{} null match", sentence);
                            } else {
                                match.category.incrementActivationCnt();
                                //System.out.println(count+". "+sentence+" matched "+match.category.inputThatTopic());
                            }
                            count += 1;
                            if (count % 10000 == 0) { logger.info(String.valueOf(count)); }
                        }
                    }
                }
            }
            logger.info("Finished classifying {} inputs", count);
            //Close the input stream
            br.close();
        } catch (Exception e) {
            logger.error("classifyInputs error", e);
        }
    }

    /**
     * magically suggests new patterns for a bot.
     * Reads an input file of sample data called logFile.
     * Builds a graph of all the inputs.
     * Finds new patterns in the graph that are not already in the bot.
     * Classifies input log into those new patterns.
     */
    public void ab() {
        String logFile = logfile;
        LogUtil.activateDebug(false);
        MagicBooleans.enable_external_sets = false;
        if (offer_alice_responses) { alice = new Bot("alice"); }
        Timer timer = new Timer();
        bot.brain.nodeStats();
        if (bot.brain.getCategories().size() < MagicNumbers.brain_print_size) { bot.brain.printgraph(); }
        timer.start();
        logger.info("Graphing inputs");
        graphInputs(logFile);
        logger.info("{} seconds Graphing inputs", timer.elapsedTimeSecs());
        inputGraph.nodeStats();
        if (inputGraph.getCategories().size() < MagicNumbers.brain_print_size) { inputGraph.printgraph(); }
        //bot.inputGraph.printgraph();
        timer.start();
        logger.info("Finding Patterns");
        findPatterns();
        logger.info("{} suggested categories", suggestedCategories.size());
        logger.info("{} seconds finding patterns", timer.elapsedTimeSecs());
        timer.start();
        patternGraph.nodeStats();
        if (patternGraph.getCategories().size() < MagicNumbers.brain_print_size) { patternGraph.printgraph(); }
        logger.info("Classifying Inputs from {}", logFile);
        classifyInputs(logFile);
        logger.info("{} classifying inputs", timer.elapsedTimeSecs());
    }

    public List<Category> nonZeroActivationCount(List<Category> suggestedCategories) {
        return suggestedCategories.stream()
            .filter(c -> c.getActivationCnt() > 0)
            .collect(Collectors.toList());
    }

    /**
     * train the bot through a terminal interaction
     */
    public void terminalInteraction() {
        sort_mode = !shuffle_mode;
        // if (sort_mode)
        Collections.sort(suggestedCategories, Category.ACTIVATION_COMPARATOR);
        ArrayList<Category> topSuggestCategories = new ArrayList<>();
        for (int i = 0; i < 10000 && i < suggestedCategories.size(); i++) {
            topSuggestCategories.add(suggestedCategories.get(i));
        }
        suggestedCategories = topSuggestCategories;
        if (shuffle_mode) { Collections.shuffle(suggestedCategories); }
        Timer timer = new Timer();
        timer.start();
        runCompletedCnt = 0;
        List<Category> filteredAtomicCategories = new ArrayList<>();
        List<Category> filteredWildCategories = new ArrayList<>();
        for (Category c : suggestedCategories) {
            if (!c.getPattern().contains("*")) {
                filteredAtomicCategories.add(c);
            } else {
                filteredWildCategories.add(c);
            }
        }
        List<Category> browserCategories;
        if (filter_atomic_mode) { browserCategories = filteredAtomicCategories; } else if (filter_wild_mode) {
            browserCategories = filteredWildCategories;
        } else {
            browserCategories = suggestedCategories;
        }
        // System.out.println(filteredAtomicCategories.size()+" filtered suggested categories");
        browserCategories = nonZeroActivationCount(browserCategories);
        boolean firstInteraction = true;
        String alicetemplate = null;
        for (Category c : browserCategories) {
            try {
                List<String> samples = new ArrayList<>(c.getMatches(bot));
                Collections.shuffle(samples);
                int sampleSize = Math.min(MagicNumbers.displayed_input_sample_size, samples.size());
                samples.stream().limit(sampleSize).forEach(logger::info);
                logger.info("[{}] {}", c.getActivationCnt(), c.inputThatTopic());
                if (offer_alice_responses) {
                    Nodemapper node = alice.brain.findNode(c);
                    if (node != null) {
                        alicetemplate = node.category.getTemplate();
                        String displayAliceTemplate = alicetemplate;
                        displayAliceTemplate = displayAliceTemplate.replace("\n", " ");
                        if (displayAliceTemplate.length() > 200) {
                            displayAliceTemplate = displayAliceTemplate.substring(0, 200);
                        }
                        logger.info("ALICE: {}", displayAliceTemplate);
                    } else {
                        alicetemplate = null;
                    }
                }

                String textLine = IOUtils.readInputTextLine();
                if (firstInteraction) {
                    timer.start();
                    firstInteraction = false;
                }
                productivity(runCompletedCnt, timer);
                terminalInteractionStep(bot, "", textLine, c, alicetemplate);
            } catch (Exception ex) {
                logger.error("terminalInteraction error", ex);
                logger.info("Returning to Category Browser");
            }
        }
        logger.info("No more samples");
        bot.writeAIMLFiles();
        bot.writeAIMLIFFiles();
    }

    /**
     * process one step of the terminal interaction
     *
     * @param bot      the bot being trained.
     * @param request  used when this routine is called by benchmark testSuite
     * @param textLine response typed by the botmaster
     * @param c        AIML category selected
     */
    public void terminalInteractionStep(Bot bot, String request, String textLine, Category c, String alicetemplate) {
        if (textLine.contains("<pattern>") && textLine.contains("</pattern>")) {
            int index = textLine.indexOf("<pattern>") + "<pattern>".length();
            int jndex = textLine.indexOf("</pattern>");
            int kndex = jndex + "</pattern>".length();
            if (index < jndex) {
                String pattern = textLine.substring(index, jndex);
                c.setPattern(pattern);
                textLine = textLine.substring(kndex, textLine.length());
                logger.info("Got pattern = {} template = {}", pattern, textLine);
            }
        }
        String botThinks = "";
        String[] pronouns = {"he", "she", "it", "we", "they"};
        for (String p : pronouns) {
            if (textLine.contains("<" + p + ">")) {
                textLine = textLine.replace("<" + p + ">", "");
                botThinks = "<think><set name=\"" + p + "\"><set name=\"topic\"><star/></set></set></think>";
            }
        }
        String template;
        if ("q".equals(textLine)) {
            System.exit(0);       // Quit program
        } else if ("wq".equals(textLine)) {   // Write AIML Files and quit program
            bot.writeQuit();
         /*  Nodemapper udcNode = bot.brain.findNode("*", "*", "*");
           if (udcNode != null) {
               AIMLSet udcMatches = new AIMLSet("udcmatches");
               udcMatches.addAll(udcNode.category.getMatches());
               udcMatches.writeAIMLSet();
           }*/
          /* Nodemapper cNode = bot.brain.match("JOE MAKES BEER", "unknown", "unknown");
           if (cNode != null) {
               AIMLSet cMatches = new AIMLSet("cmatches");
               cMatches.addAll(cNode.category.getMatches());
               cMatches.writeAIMLSet();
           }
           if (passed.size() > 0) {
               AIMLSet difference = new AIMLSet("difference");
               AIMLSet intersection = new AIMLSet("intersection");
               for (String s : passed) if (testSet.contains(s)) intersection.add(s);
               passed = intersection;
               passed.setName = "passed";
               difference.addAll(testSet);
               difference.removeAll(passed);
               difference.writeAIMLSet();

               passed.writeAIMLSet();
               testSet.writeAIMLSet();
               System.out.println("Wrote passed test cases");
           }*/
            System.exit(0);
        } else if ("skip".equals(textLine) || textLine.isEmpty()) { // skip this one for now
            skipCategory(c);
        } else if ("s".equals(textLine) || "pass".equals(textLine)) { //
            passed.add(request);
            AIMLSet difference = new AIMLSet("difference", bot);
            difference.addAll(testSet);
            difference.removeAll(passed);
            difference.writeAIMLSet();
            passed.writeAIMLSet();
        } else if ("a".equals(textLine)) {
            template = alicetemplate;
            String filename;
            if (template.contains("<sr")) {
                filename = MagicStrings.reductions_update_aiml_file;
            } else {
                filename = MagicStrings.personality_aiml_file;
            }
            saveCategory(c.getPattern(), template, filename);
        } else if ("d".equals(textLine)) { // delete this suggested category
            deleteCategory(c);
        } else if ("x".equals(textLine)) {    // ask another bot
            template = "<sraix services=\"pannous\">" + c.getPattern().replace("*", "<star/>") + "</sraix>";
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.sraix_aiml_file);
        } else if ("p".equals(textLine)) {   // filter inappropriate content
            template = "<srai>" + MagicStrings.inappropriate_filter + "</srai>";
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.inappropriate_aiml_file);
        } else if ("f".equals(textLine)) { // filter profanity
            template = "<srai>" + MagicStrings.profanity_filter + "</srai>";
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.profanity_aiml_file);
        } else if ("i".equals(textLine)) {
            template = "<srai>" + MagicStrings.insult_filter + "</srai>";
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.insult_aiml_file);
        } else if (textLine.contains("<srai>") || textLine.contains("<sr/>")) {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.reductions_update_aiml_file);
        } else if (textLine.contains("<oob>")) {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.oob_aiml_file);
        } else if (textLine.contains("<set name") || !botThinks.isEmpty()) {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.predicates_aiml_file);
        } else if (textLine.contains("<get name") && !textLine.contains("<get name=\"name")) {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.predicates_aiml_file);
        } else {
            template = textLine;
            template += botThinks;
            saveCategory(c.getPattern(), template, MagicStrings.personality_aiml_file);
        }

    }

}

