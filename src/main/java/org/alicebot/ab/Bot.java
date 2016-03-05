package org.alicebot.ab;
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class representing the AIML bot
 */
public final class Bot {

    private static final Logger logger = LoggerFactory.getLogger(Bot.class);

    public final Properties properties = new Properties();
    public final PreProcessor preProcessor;
    public final Graphmaster brain;
    public Graphmaster learnfGraph;
    public Graphmaster learnGraph;

    // public Graphmaster unfinishedGraph;
    //  public final ArrayList<Category> categories;

    public String name = MagicStrings.default_bot_name;
    public Map<String, AIMLSet> setMap = new HashMap<>();
    public Map<String, AIMLMap> mapMap = new HashMap<>();
    public Set<String> pronounSet = new HashSet<>();
    public String root_path = "c:/ab";
    public String bot_path = root_path + "/bots";
    public String bot_name_path = bot_path + "/super";
    public String aimlif_path = bot_path + "/aimlif";
    public String aiml_path = bot_path + "/aiml";
    public String config_path = bot_path + "/config";
    public String log_path = bot_path + "/log";
    public String sets_path = bot_path + "/sets";
    public String maps_path = bot_path + "/maps";

    /**
     * Set all directory path variables for this bot
     *
     * @param root root directory of Program AB
     * @param name name of bot
     */
    public void setAllPaths(String root, String name) {
        bot_path = root + "/bots";
        bot_name_path = bot_path + "/" + name;
        logger.debug("Name = {} Path = {}", name, bot_name_path);
        aiml_path = bot_name_path + "/aiml";
        aimlif_path = bot_name_path + "/aimlif";
        config_path = bot_name_path + "/config";
        log_path = bot_name_path + "/logs";
        sets_path = bot_name_path + "/sets";
        maps_path = bot_name_path + "/maps";
        logger.debug(root_path);
        logger.debug(bot_path);
        logger.debug(bot_name_path);
        logger.debug(aiml_path);
        logger.debug(aimlif_path);
        logger.debug(config_path);
        logger.debug(log_path);
        logger.debug(sets_path);
        logger.debug(maps_path);
    }

    /**
     * Constructor (default action, default path, default bot name)
     */
    public Bot() {
        this(MagicStrings.default_bot);
    }

    /**
     * Constructor (default action, default path)
     */
    public Bot(String name) {
        this(name, MagicStrings.root_path);
    }

    /**
     * Constructor (default action)
     */
    public Bot(String name, String path) {
        this(name, path, "auto");
    }

    /**
     * Constructor
     *
     * @param name   name of bot
     * @param path   root path of Program AB
     * @param action Program AB action
     */
    public Bot(String name, String path, String action) {
        this.name = name;
        setAllPaths(path, name);
        this.brain = new Graphmaster(this);

        this.learnfGraph = new Graphmaster(this, "learnf");
        this.learnGraph = new Graphmaster(this, "learn");
        //      this.unfinishedGraph = new Graphmaster(this);
        //  this.categories = new ArrayList<Category>();

        preProcessor = new PreProcessor(this);
        addProperties();
        long setCnt = addAIMLSets();
        logger.debug("Loaded {} set elements.", setCnt);
        long mapCnt = addAIMLMaps();
        logger.debug("Loaded {} map elements", mapCnt);
        this.pronounSet = getPronouns();
        AIMLSet number = new AIMLSet(MagicStrings.natural_number_set_name);
        setMap.put(MagicStrings.natural_number_set_name, number);
        AIMLMap successor = new AIMLMap(MagicStrings.map_successor);
        mapMap.put(MagicStrings.map_successor, successor);
        AIMLMap predecessor = new AIMLMap(MagicStrings.map_predecessor);
        mapMap.put(MagicStrings.map_predecessor, predecessor);
        AIMLMap singular = new AIMLMap(MagicStrings.map_singular);
        mapMap.put(MagicStrings.map_singular, singular);
        AIMLMap plural = new AIMLMap(MagicStrings.map_plural);
        mapMap.put(MagicStrings.map_plural, plural);
        //System.out.println("setMap = "+setMap);
        Instant aimlDate = Instant.ofEpochMilli(new File(aiml_path).lastModified());
        Instant aimlIFDate = Instant.ofEpochMilli(new File(aimlif_path).lastModified());
        logger.debug("AIML modified {} AIMLIF modified {}", aimlDate, aimlIFDate);
        //readUnfinishedIFCategories();
        MagicStrings.pannous_api_key = Utilities.getPannousAPIKey(this);
        MagicStrings.pannous_login = Utilities.getPannousLogin(this);
        if ("aiml2csv".equals(action)) {
            addCategoriesFromAIML();
        } else if ("csv2aiml".equals(action)) {
            addCategoriesFromAIMLIF();
        } else if ("chat-app".equals(action)) {
            logger.debug("Loading only AIMLIF files");
            addCategoriesFromAIMLIF();
        } else if (aimlDate.isAfter(aimlIFDate)) {
            logger.debug("AIML modified after AIMLIF");
            addCategoriesFromAIML();
            writeAIMLIFFiles();
        } else {
            addCategoriesFromAIMLIF();
            if (brain.getCategories().isEmpty()) {
                logger.info("No AIMLIF Files found.  Looking for AIML");
                addCategoriesFromAIML();
            }
        }
        Category b = new Category(0, "PROGRAM VERSION", "*", "*", MagicStrings.program_name_version, "update.aiml");
        brain.addCategory(b);
        brain.nodeStats();
        learnfGraph.nodeStats();

    }

    Set<String> getPronouns() {
        Set<String> pronounSet = Utilities.lines(new File(config_path, "pronouns.txt"))
            .map(String::trim).filter(p -> !p.isEmpty()).collect(Collectors.toSet());
        logger.debug("Read pronouns: {}", pronounSet);
        return pronounSet;
    }

    /**
     * add an array list of categories with a specific file name
     *
     * @param file           name of AIML file
     * @param moreCategories list of categories
     */
    void addMoreCategories(String file, Iterable<Category> moreCategories) {
        if (file.contains(MagicStrings.deleted_aiml_file)) {
           /* for (Category c : moreCategories) {
                //System.out.println("Delete "+c.getPattern());
                deletedGraph.addCategory(c);
            }*/

        } else if (file.contains(MagicStrings.learnf_aiml_file)) {
            logger.debug("Reading Learnf file");
            for (Category c : moreCategories) {
                brain.addCategory(c);
                learnfGraph.addCategory(c);
                //patternGraph.addCategory(c);
            }
            //this.categories.addAll(moreCategories);
        } else {
            for (Category c : moreCategories) {
                //System.out.println("Brain size="+brain.root.size());
                //brain.printgraph();
                brain.addCategory(c);
                //patternGraph.addCategory(c);
                //brain.printgraph();
            }
            //this.categories.addAll(moreCategories);
        }
    }

    /**
     * Load all brain categories from AIML directory
     */
    int addCategoriesFromAIML() {
        Timer timer = new Timer();
        timer.start();
        int cnt = 0;
        try {
            // Directory path here
            File folder = new File(aiml_path);
            if (folder.exists()) {
                File[] listOfFiles = IOUtils.listFiles(folder);
                logger.debug("Loading AIML files from {}", aiml_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        String file = listOfFile.getName();
                        if (file.endsWith(".aiml") || file.endsWith(".AIML")) {
                            logger.debug(file);
                            try {
                                List<Category> moreCategories = AIMLProcessor.AIMLToCategories(aiml_path, file);
                                addMoreCategories(file, moreCategories);
                                cnt += moreCategories.size();
                            } catch (Exception iex) {
                                logger.error("problem loading {}", file, iex);
                            }
                        }
                    }
                }
            } else {
                logger.warn("addCategoriesFromAIML: {} does not exist.", folder);
            }
        } catch (Exception ex) {
            logger.error("addCategoriesFromAIML error", ex);
        }
        logger.debug("Loaded {} categories in {} sec", cnt, timer.elapsedTimeSecs());
        return cnt;
    }

    /**
     * load all brain categories from AIMLIF directory
     */
    public int addCategoriesFromAIMLIF() {
        Timer timer = new Timer();
        timer.start();
        int cnt = 0;
        try {
            // Directory path here
            File folder = new File(aimlif_path);
            if (folder.exists()) {
                File[] listOfFiles = IOUtils.listFiles(folder);
                logger.debug("Loading AIML files from {}", aimlif_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        String file = listOfFile.getName();
                        if (file.endsWith(MagicStrings.aimlif_file_suffix) || file.endsWith(MagicStrings.aimlif_file_suffix.toUpperCase())) {
                            logger.debug(file);
                            try {
                                List<Category> moreCategories = readIFCategories(aimlif_path + "/" + file);
                                cnt += moreCategories.size();
                                addMoreCategories(file, moreCategories);
                                //   MemStats.memStats();
                            } catch (Exception iex) {
                                logger.error("Problem loading {}", file, iex);
                            }
                        }
                    }
                }
            } else {
                logger.warn("addCategoriesFromAIMLIF: {} does not exist.", folder);
            }
        } catch (Exception ex) {
            logger.error("addCategoriesFromAIMLIF error", ex);
        }
        logger.debug("Loaded {} categories in {} sec", cnt, timer.elapsedTimeSecs());
        return cnt;
    }

    /**
     * write all AIML and AIMLIF categories
     */
    public void writeQuit() {
        writeAIMLIFFiles();
        //System.out.println("Wrote AIMLIF Files");
        writeAIMLFiles();
        //System.out.println("Wrote AIML Files");
        /*  updateUnfinishedCategories();
        writeUnfinishedIFCategories();
*/
    }

    /**
     * read categories from specified AIMLIF file into specified graph
     *
     * @param graph    Graphmaster to store categories
     * @param fileName file name of AIMLIF file
     */
    public void readCertainIFCategories(Graphmaster graph, String fileName) {
        File file = new File(aimlif_path, fileName + MagicStrings.aimlif_file_suffix);
        if (file.exists()) {
            try {
                List<Category> certainCategories = readIFCategories(aimlif_path + "/" + fileName + MagicStrings.aimlif_file_suffix);
                certainCategories.forEach(graph::addCategory);
                int cnt = certainCategories.size();
                logger.info("readCertainIFCategories {} categories from {}", cnt, file);
            } catch (Exception iex) {
                logger.error("Problem loading {}", file, iex);
            }
        } else {
            logger.warn("No {} file found", file);
        }
    }

    /**
     * write certain specified categories as AIMLIF files
     *
     * @param graph the Graphmaster containing the categories to write
     * @param file  the destination AIMLIF file
     */
    public void writeCertainIFCategories(Graphmaster graph, String file) {
        logger.debug("writeCertainIFCaegories {} size= {}", file, graph.getCategories().size());
        writeIFCategories(graph.getCategories(), file + MagicStrings.aimlif_file_suffix);
        File dir = new File(aimlif_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * write learned categories to AIMLIF file
     */
    public void writeLearnfIFCategories() {
        writeCertainIFCategories(learnfGraph, MagicStrings.learnf_aiml_file);
    }

    /**
     * write unfinished categories to AIMLIF file
     */
   /* public void writeUnfinishedIFCategories() {
        writeCertainIFCategories(unfinishedGraph, MagicStrings.unfinished_aiml_file);
    }*/

    /**
     * write categories to AIMLIF file
     *
     * @param cats     array list of categories
     * @param filename AIMLIF filename
     */
    public void writeIFCategories(Iterable<Category> cats, String filename) {
        //System.out.println("writeIFCategories "+filename);
        File existsPath = new File(aimlif_path);
        if (existsPath.exists()) {
            BufferedWriter bw = null;
            try {
                //Construct the bw object
                bw = new BufferedWriter(new FileWriter(aimlif_path + "/" + filename));
                for (Category category : cats) {
                    bw.write(Category.categoryToIF(category));
                    bw.newLine();
                }
            } catch (IOException ex) {
                logger.error("writeIFCategories error", ex);
            } finally {
                //Close the bw
                try {
                    if (bw != null) {
                        bw.flush();
                        bw.close();
                    }
                } catch (IOException ex) {
                    logger.error("writeIFCategories error", ex);
                }
            }
        }
    }

    /**
     * Write all AIMLIF files from bot brain
     */
    public void writeAIMLIFFiles() {
        logger.debug("writeAIMLIFFiles");
        Map<String, BufferedWriter> fileMap = new HashMap<>();
        Category b = new Category(0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
        brain.addCategory(b);
        List<Category> brainCategories = brain.getCategories();
        Collections.sort(brainCategories, Category.CATEGORY_NUMBER_COMPARATOR);
        for (Category c : brainCategories) {
            try {
                BufferedWriter bw;
                String fileName = c.getFilename();
                if (fileMap.containsKey(fileName)) {
                    bw = fileMap.get(fileName);
                } else {
                    bw = new BufferedWriter(new FileWriter(aimlif_path + "/" + fileName + MagicStrings.aimlif_file_suffix));
                    fileMap.put(fileName, bw);

                }
                bw.write(Category.categoryToIF(c));
                bw.newLine();
            } catch (Exception ex) {
                logger.error("writeAIMLIFFiles error", ex);
            }
        }
        for (BufferedWriter bw : fileMap.values()) {
            //Close the bw
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                logger.error("writeAIMLIFFiles error", ex);
            }

        }
        File dir = new File(aimlif_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * Write all AIML files.  Adds categories for BUILD and DEVELOPMENT ENVIRONMENT
     */
    public void writeAIMLFiles() {
        logger.debug("writeAIMLFiles");
        Map<String, BufferedWriter> fileMap = new HashMap<>();
        Category b = new Category(0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
        brain.addCategory(b);
        //b = new Category(0, "PROGRAM VERSION", "*", "*", MagicStrings.program_name_version, "update.aiml");
        //brain.addCategory(b);
        List<Category> brainCategories = brain.getCategories();
        Collections.sort(brainCategories, Category.CATEGORY_NUMBER_COMPARATOR);
        for (Category c : brainCategories) {

            if (!c.getFilename().equals(MagicStrings.null_aiml_file)) {
                try {
                    //System.out.println("Writing "+c.getCategoryNumber()+" "+c.inputThatTopic());
                    BufferedWriter bw;
                    String fileName = c.getFilename();
                    if (fileMap.containsKey(fileName)) {
                        bw = fileMap.get(fileName);
                    } else {
                        String copyright = Utilities.getCopyright(this, fileName);
                        bw = new BufferedWriter(new FileWriter(aiml_path + "/" + fileName));
                        fileMap.put(fileName, bw);
                        bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                            "<aiml>\n");
                        bw.write(copyright);
                        //bw.newLine();
                    }
                    bw.write(Category.categoryToAIML(c) + "\n");
                    //bw.newLine();
                } catch (Exception ex) {
                    logger.error("writeAIMLFiles error", ex);
                }
            }
        }
        for (BufferedWriter bw : fileMap.values()) {
            //Close the bw
            try {
                if (bw != null) {
                    bw.write("</aiml>\n");
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
                logger.error("writeAIMLFiles error", ex);
            }

        }
        File dir = new File(aiml_path);
        dir.setLastModified(new Date().getTime());
    }

    /**
     * load bot properties
     */
    void addProperties() {
        try {
            properties.getProperties(config_path + "/properties.txt");
        } catch (Exception ex) {
            logger.error("addProperties error", ex);
        }
    }

    /**
     * read AIMLIF categories from a file into bot brain
     *
     * @param filename name of AIMLIF file
     * @return array list of categories read
     */
    public List<Category> readIFCategories(String filename) {
        try {
            return Files.lines(new File(filename).toPath()).map(strLine -> {
                try {
                    return Category.IFToCategory(strLine);
                } catch (Exception ex) {
                    logger.error("Invalid AIMLIF in {} line {}", filename, strLine, ex);
                    return null;
                }
            }).filter(c -> c != null).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("readIFCategories error", e);
            return Collections.emptyList();
        }
    }

    /**
     * Load all AIML Sets
     */
    long addAIMLSets() {
        Timer timer = new Timer();
        timer.start();
        long cnt = 0;
        try {
            // Directory path here
            File folder = new File(sets_path);
            if (folder.exists()) {
                File[] listOfFiles = IOUtils.listFiles(folder);
                logger.debug("Loading AIML Sets files from {}", sets_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        String file = listOfFile.getName();
                        if (file.endsWith(".txt") || file.endsWith(".TXT")) {
                            logger.debug(file);
                            String setName = file.substring(0, file.length() - ".txt".length());
                            logger.debug("Read AIML Set {}", setName);
                            AIMLSet aimlSet = new AIMLSet(setName);
                            cnt += aimlSet.readSet(this);
                            setMap.put(setName, aimlSet);
                        }
                    }
                }
            } else {
                logger.warn("addAIMLSets: {} does not exist.", folder);
            }
        } catch (Exception ex) {
            logger.error("addAIMLSets error", ex);
        }
        return cnt;
    }

    /**
     * Load all AIML Maps
     */
    long addAIMLMaps() {
        Timer timer = new Timer();
        timer.start();
        long cnt = 0;
        try {
            // Directory path here
            File folder = new File(maps_path);
            if (folder.exists()) {
                File[] listOfFiles = IOUtils.listFiles(folder);
                logger.debug("Loading AIML Map files from {}", maps_path);
                for (File listOfFile : listOfFiles) {
                    if (listOfFile.isFile()) {
                        String file = listOfFile.getName();
                        if (file.endsWith(".txt") || file.endsWith(".TXT")) {
                            logger.debug(file);
                            String mapName = file.substring(0, file.length() - ".txt".length());
                            logger.debug("Read AIML Map {}", mapName);
                            AIMLMap aimlMap = new AIMLMap(mapName);
                            cnt += aimlMap.readMap(this);
                            mapMap.put(mapName, aimlMap);
                        }
                    }
                }
            } else {
                logger.warn("addAIMLMaps: {} does not exist.", folder);
            }
        } catch (Exception ex) {
            logger.warn("addAIMLMaps error", ex);
        }
        return cnt;
    }

    public void deleteLearnfCategories() {
        Iterable<Category> learnfCategories = learnfGraph.getCategories();
        for (Category c : learnfCategories) {
            Nodemapper n = brain.findNode(c);
            logger.info("Found node {} for {}", n, c.inputThatTopic());
            if (n != null) { n.category = null; }
        }
        learnfGraph = new Graphmaster(this);
    }

    public void deleteLearnCategories() {
        Iterable<Category> learnCategories = learnGraph.getCategories();
        for (Category c : learnCategories) {
            Nodemapper n = brain.findNode(c);
            logger.info("Found node {} for {}", n, c.inputThatTopic());
            if (n != null) { n.category = null; }
        }
        learnGraph = new Graphmaster(this);
    }

    /**
     * check Graphmaster for shadowed categories
     */
    public void shadowChecker() {
        shadowChecker(brain.root);
    }

    /**
     * traverse graph and test all categories found in leaf nodes for shadows
     */
    void shadowChecker(Nodemapper node) {
        if (NodemapperOperator.isLeaf(node)) {
            String input = node.category.getPattern();
            input = brain.replaceBotProperties(input);
            input =
                input.replace("*", "XXX").replace("_", "XXX").replace("^", "").replace("#", "");
            String that = node.category.getThat().replace("*", "XXX").replace("_", "XXX").replace("^", "").replace("#", "");
            String topic = node.category.getTopic().replace("*", "XXX").replace("_", "XXX").replace("^", "").replace("#", "");
            input = instantiateSets(input);
            logger.info("shadowChecker: input={}", input);
            Nodemapper match = brain.match(input, that, topic);
            if (!Objects.equals(match, node)) {
                logger.info(Graphmaster.inputThatTopic(input, that, topic));
                logger.info("MATCHED:     {}", match.category.inputThatTopic());
                logger.info("SHOULD MATCH:{}", node.category.inputThatTopic());
            }
        } else {
            for (String key : NodemapperOperator.keySet(node)) {
                shadowChecker(NodemapperOperator.get(node, key));
            }
        }
    }

    public String instantiateSets(String pattern) {
        String[] splitPattern = pattern.split(" ");
        pattern = "";
        for (String x : splitPattern) {
            if (x.startsWith("<SET>")) {
                String setName = AIMLProcessor.trimTag(x, "SET");
                AIMLSet set = setMap.get(setName);
                x = set != null ? "FOUNDITEM" : "NOTFOUND";
            }
            pattern = pattern + " " + x;
        }
        return pattern.trim();
    }
}
