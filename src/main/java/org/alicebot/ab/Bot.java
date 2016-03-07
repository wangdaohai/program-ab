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

import org.alicebot.ab.map.AIMLMap;
import org.alicebot.ab.map.AIMLMapBuilder;
import org.alicebot.ab.map.ComputeMap;
import org.alicebot.ab.set.AIMLSet;
import org.alicebot.ab.set.AIMLSetBuilder;
import org.alicebot.ab.set.ComputeSet;
import org.alicebot.ab.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final Path aimlifPath;
    private final Path aimlPath;
    public final Path configPath;
    public final Path logPath;
    public final Path setsPath;
    public final Path mapsPath;

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
        this(name, MagicStrings.rootPath);
    }

    /**
     * Constructor (default action)
     */
    public Bot(String name, java.nio.file.Path path) {
        this(name, path, "auto");
    }

    /**
     * Constructor
     *
     * @param name     name of bot
     * @param rootPath root path of Program AB
     * @param action   Program AB action
     */
    public Bot(String name, java.nio.file.Path rootPath, String action) {
        this.name = name;
        Path botPath = rootPath.resolve("bots").resolve(name);
        logger.debug("Name = {} Path = {}", name, botPath);
        aimlPath = botPath.resolve("aiml");
        aimlifPath = botPath.resolve("aimlif");
        configPath = botPath.resolve("config");
        logPath = botPath.resolve("logs");
        setsPath = botPath.resolve("sets");
        mapsPath = botPath.resolve("maps");
        logger.debug("Bot path: {}", botPath);

        this.brain = new Graphmaster(this);

        this.learnfGraph = new Graphmaster(this, "learnf");
        this.learnGraph = new Graphmaster(this, "learn");
        //      this.unfinishedGraph = new Graphmaster(this);
        //  this.categories = new ArrayList<Category>();

        preProcessor = new PreProcessor(this);
        addProperties();
        addAIMLSets();
        addAIMLMaps();
        this.pronounSet = getPronouns();
        AIMLSet number = ComputeSet.NATURAL_NUMBERS;
        setMap.put(number.name(), number);
        for (AIMLMap map : new AIMLMap[]{
            ComputeMap.SUCCESSOR, ComputeMap.PREDECESSOR,
            ComputeMap.SINGULAR, ComputeMap.PLURAL
        }) {
            mapMap.put(map.name(), map);
        }

        Instant aimlDate = Instant.ofEpochMilli(aimlPath.toFile().lastModified());
        Instant aimlIFDate = Instant.ofEpochMilli(aimlifPath.toFile().lastModified());
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
        Set<String> pronounSet = Utilities.lines(configPath.resolve("pronouns.txt"))
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
    void addCategoriesFromAIML() {
        try {
            Timer timer = new Timer();
            timer.start();
            File folder = aimlPath.toFile();
            if (folder.exists()) {
                logger.debug("Loading AIML files from {}", aimlPath);
                int cnt = IOUtils.filesWithExtension(aimlPath, ".aiml")
                    .mapToInt(path -> {
                        String file = path.getFileName().toString();
                        logger.debug(file);
                        try {
                            List<Category> moreCategories = AIMLProcessor.AIMLToCategories(aimlPath, file);
                            addMoreCategories(file, moreCategories);
                            return moreCategories.size();
                        } catch (Exception iex) {
                            logger.error("problem loading categories from {}", path, iex);
                            return 0;
                        }
                    }).sum();
                logger.debug("Loaded {} categories in {} sec", cnt, timer.elapsedTimeSecs());
            } else {
                logger.warn("addCategoriesFromAIML: {} does not exist.", folder);
            }
        } catch (Exception ex) {
            logger.error("addCategoriesFromAIML error", ex);
        }
    }

    /**
     * load all brain categories from AIMLIF directory
     */
    private void addCategoriesFromAIMLIF() {
        try {
            Timer timer = new Timer();
            timer.start();
            // Directory path here
            File folder = aimlifPath.toFile();
            if (folder.exists()) {
                logger.debug("Loading AIML files from {}", aimlifPath);
                int cnt = IOUtils.filesWithExtension(aimlifPath, MagicStrings.aimlif_file_suffix)
                    .mapToInt(path -> {
                        String file = path.getFileName().toString();
                        logger.debug(file);
                        try {
                            List<Category> moreCategories = readIFCategories(aimlifPath.resolve(file));
                            addMoreCategories(file, moreCategories);
                            //MemStats.memStats();
                            return moreCategories.size();
                        } catch (Exception iex) {
                            logger.error("Problem loading categories from {}", path, iex);
                            return 0;
                        }
                    }).sum();
                logger.debug("Loaded {} categories in {} sec", cnt, timer.elapsedTimeSecs());
            } else {
                logger.warn("addCategoriesFromAIMLIF: {} does not exist.", folder);
            }
        } catch (Exception ex) {
            logger.error("addCategoriesFromAIMLIF error", ex);
        }
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
        File file = aimlifPath.resolve(fileName + MagicStrings.aimlif_file_suffix).toFile();
        if (file.exists()) {
            try {
                List<Category> certainCategories = readIFCategories(aimlifPath.resolve(fileName + MagicStrings.aimlif_file_suffix));
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
        aimlifPath.toFile().setLastModified(new Date().getTime());
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
    public void writeIFCategories(List<Category> cats, String filename) {
        //System.out.println("writeIFCategories "+filename);
        if (aimlifPath.toFile().exists()) {
            try {
                Files.write(aimlifPath.resolve(filename),
                    (Iterable<String>) cats.stream().map(Category::categoryToIF)::iterator);
            } catch (IOException ex) {
                logger.error("writeIFCategories error", ex);
            }
        }
    }

    /**
     * Write all AIMLIF files from bot brain
     */
    public void writeAIMLIFFiles() {
        logger.debug("writeAIMLIFFiles");
        Category b = new Category(0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
        brain.addCategory(b);
        brain.getCategories().stream().sorted(Category.CATEGORY_NUMBER_COMPARATOR)
            .collect(Collectors.groupingBy(Category::getFilename)).entrySet().stream()
            .forEach(entry -> {
                try {
                    Stream<String> categoriesIF = entry.getValue().stream().map(Category::categoryToIF);
                    Files.write(aimlifPath.resolve(entry.getKey() + MagicStrings.aimlif_file_suffix),
                        (Iterable<String>) categoriesIF::iterator);
                } catch (IOException e) {
                    logger.error("writeAIMLIFFiles error", e);
                }
            });
        aimlifPath.toFile().setLastModified(new Date().getTime());
    }

    /**
     * Write all AIML files.  Adds categories for BUILD and DEVELOPMENT ENVIRONMENT
     */
    public void writeAIMLFiles() {
        logger.debug("writeAIMLFiles");
        Category b = new Category(0, "BRAIN BUILD", "*", "*", new Date().toString(), "update.aiml");
        brain.addCategory(b);
        //b = new Category(0, "PROGRAM VERSION", "*", "*", MagicStrings.program_name_version, "update.aiml");
        //brain.addCategory(b);

        brain.getCategories().stream().sorted(Category.CATEGORY_NUMBER_COMPARATOR)
            .collect(Collectors.groupingBy(Category::getFilename)).entrySet().stream()
            .forEach(entry -> {
                try {
                    Files.write(aimlPath.resolve(entry.getKey()),
                        aimlFileContent(entry.getKey(), entry.getValue()));
                } catch (IOException e) {
                    logger.error("writeAIMLFiles error", e);
                }
            });
        aimlPath.toFile().setLastModified(new Date().getTime());
    }

    Iterable<String> aimlFileContent(String fileName, List<Category> categories) {
        return Stream.of(
            Stream.of("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<aiml>"),
            Stream.of(Utilities.getCopyright(this, fileName)),
            categories.stream().map(Category::categoryToAIML),
            Stream.of("</aiml>")
        ).flatMap(a -> a)::iterator;
    }

    /**
     * load bot properties
     */
    void addProperties() {
        try {
            properties.getProperties(configPath.resolve("properties.txt"));
        } catch (Exception ex) {
            logger.error("addProperties error", ex);
        }
    }

    /**
     * read AIMLIF categories from a file into bot brain
     *
     * @param path name of AIMLIF file
     * @return array list of categories read
     */
    public List<Category> readIFCategories(Path path) {
        try {
            return Files.lines(path).map(strLine -> {
                try {
                    return Category.IFToCategory(strLine);
                } catch (Exception ex) {
                    logger.error("Invalid AIMLIF in {} line {}", path, strLine, ex);
                    return null;
                }
            }).filter(c -> c != null).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("error reading categories from {}", path, e);
            return Collections.emptyList();
        }
    }

    /** Load all AIML Sets */
    void addAIMLSets() {
        try {
            setMap.putAll(AIMLSetBuilder.fromFolder(setsPath)
                .collect(Collectors.toMap(AIMLSet::name, s -> s)));
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded {} set elements in {} sets.",
                    setMap.values().stream().mapToInt(AIMLSet::size).sum(),
                    setMap.size());
            }
        } catch (Exception ex) {
            logger.error("addAIMLSets error", ex);
        }
    }

    /** Load all AIML Maps */
    void addAIMLMaps() {
        try {
            mapMap.putAll(AIMLMapBuilder.fromFolder(mapsPath)
                .collect(Collectors.toMap(AIMLMap::name, m -> m)));
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded {} map elements in {} maps",
                    mapMap.values().stream().mapToInt(AIMLMap::size).sum(),
                    mapMap.size());
            }
        } catch (Exception ex) {
            logger.warn("addAIMLMaps error", ex);
        }
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
        if (node.isLeaf()) {
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
            for (String key : node.keySet()) {
                shadowChecker(node.get(key));
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
