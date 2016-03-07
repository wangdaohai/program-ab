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

import org.alicebot.ab.set.AIMLSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The AIML Pattern matching algorithm and data structure.
 */
public class Graphmaster {

    private static final Logger logger = LoggerFactory.getLogger(Graphmaster.class);

    private static final boolean DEBUG = false;

    public Bot bot;
    public String name;
    public final Nodemapper root;
    public int matchCount = 0;
    public int upgradeCnt = 0;
    public Set<String> vocabulary;
    public String resultNote = "";
    public int categoryCnt = 0;
    public static boolean enableShortCuts = false;

    /**
     * Constructor
     *
     * @param bot the bot the graph belongs to.
     */
    public Graphmaster(Bot bot) {
        this(bot, "brain");
    }

    public Graphmaster(Bot bot, String name) {
        root = new Nodemapper();
        this.bot = bot;
        this.name = name;
        vocabulary = new HashSet<>();
    }

    /**
     * Convert input, that and topic to a single sentence having the form
     * {@code input <THAT> that <TOPIC> topic}
     *
     * @param input input (or input pattern)
     * @param that  that (or that pattern)
     * @param topic topic (or topic pattern)
     */
    public static String inputThatTopic(String input, String that, String topic) {
        return input.trim() + " <THAT> " + that.trim() + " <TOPIC> " + topic.trim();
    }

    String botPropRegex = "<bot name=\"(.*?)\"/>";
    Pattern botPropPattern = Pattern.compile(botPropRegex, Pattern.CASE_INSENSITIVE);

    public String replaceBotProperties(String pattern) {
        if (pattern.contains("<B")) {
            Matcher matcher = botPropPattern.matcher(pattern);
            while (matcher.find()) {
                String propname = matcher.group(1).toLowerCase();
                //System.out.println(matcher.group(1));
                String property = bot.properties.get(propname).toUpperCase();
                pattern = pattern.replaceFirst("(?i)" + botPropRegex, property);
                //System.out.println("addCategory: Replaced pattern with: "+inputThatTopic);
            }

        }
        return pattern;
    }

    /**
     * add an AIML category to this graph.
     *
     * @param category AIML Category
     */
    public void addCategory(Category category) {
        String inputThatTopic = inputThatTopic(category.getPattern(), category.getThat(), category.getTopic());
        //System.out.println("addCategory: "+inputThatTopic);
        inputThatTopic = replaceBotProperties(inputThatTopic);
        /*if (inputThatTopic.contains("<B")) {
        Matcher matcher = botPropPattern.matcher(inputThatTopic);
        while (matcher.find()) {
            String propname = matcher.group(1).toLowerCase();
            //System.out.println(matcher.group(1));
            String property = bot.properties.get(propname).toUpperCase();
            inputThatTopic = inputThatTopic.replaceFirst("(?i)"+botPropRegex, property);
            //System.out.println("addCategory: Replaced pattern with: "+inputThatTopic);
        }
        }*/
        //
        Path p = Path.sentenceToPath(inputThatTopic);
        addPath(p, category);
        categoryCnt++;
    }

    boolean thatStarTopicStar(Path path) {
        String tail = Path.pathToSentence(path).trim();
        //System.out.println("thatStarTopicStar "+tail+" "+tail.equals("<THAT> * <TOPIC> *"));
        return "<THAT> * <TOPIC> *".equals(tail);
    }

    void addSets(String type, Bot bot, Nodemapper node, String filename) {
        //System.out.println("adding Set "+type+" from "+bot.setMap);
        String setName = Utilities.tagTrim(type, "SET").toLowerCase();
        //AIMLSet aimlSet;
        if (bot.setMap.containsKey(setName)) {
            if (node.sets == null) { node.sets = new ArrayList<>(); }
            if (!node.sets.contains(setName)) { node.sets.add(setName); }
            //System.out.println("sets = "+node.sets);
        } else {
            logger.info("No AIML Set found for <set>{}</set> in {} {}", setName, bot.name, filename);
        }
    }

    /**
     * add a path to the graph from the root to a Category
     *
     * @param path     Pattern path
     * @param category AIML category
     */
    void addPath(Path path, Category category) {
        addPath(root, path, category);

    }

    /**
     * add a Path to the graph from a given node.
     * Shortcuts: Replace all instances of paths "<THAT> * <TOPIC> *" with a direct link to the matching category
     *
     * @param node     starting node in graph
     * @param path     Pattern path to be added
     * @param category AIML Category
     */
    void addPath(Nodemapper node, Path path, Category category) {
        //if (path != null) System.out.println("Enable shortcuts = "+enableShortCuts+" path="+Path.pathToSentence(path)+" "+thatStarTopicStar(path));
        if (path == null) {
            node.category = category;
            node.height = 0;
        } else if (enableShortCuts && thatStarTopicStar(path)) {
            node.category = category;
            node.height = Math.min(4, node.height);
            node.shortCut = true;
        } else if (node.containsKey(path.word)) {
            if (path.word.startsWith("<SET>")) { addSets(path.word, bot, node, category.getFilename()); }
            Nodemapper nextNode = node.get(path.word);
            addPath(nextNode, path.next, category);
            int offset = 1;
            if ("#".equals(path.word) || "^".equals(path.word)) { offset = 0; }
            node.height = Math.min(offset + nextNode.height, node.height);
        } else {
            Nodemapper nextNode = new Nodemapper();
            if (path.word.startsWith("<SET>")) {
                addSets(path.word, bot, node, category.getFilename());
            }
            if (node.isSingleton()) {
                node.upgrade();
                upgradeCnt++;
            }
            node.put(path.word, nextNode);
            addPath(nextNode, path.next, category);
            int offset = 1;
            if ("#".equals(path.word) || "^".equals(path.word)) { offset = 0; }
            node.height = Math.min(offset + nextNode.height, node.height);
        }
    }

    /**
     * test if category is already in graph
     *
     * @return true or false
     */
    public boolean existsCategory(Category c) {
        return (findNode(c) != null);
    }

    /**
     * test if category is already in graph
     *
     * @return true or false
     */
    public Nodemapper findNode(Category c) {
        return findNode(c.getPattern(), c.getThat(), c.getTopic());
    }

    /**
     * Given an input pattern, that pattern and topic pattern, find the leaf node associated with this path.
     *
     * @param input input pattern
     * @param that  that pattern
     * @param topic topic pattern
     * @return leaf node or null if no matching node is found
     */
    public Nodemapper findNode(String input, String that, String topic) {
        Nodemapper result = findNode(root, Path.sentenceToPath(inputThatTopic(input, that, topic)));
        if (verbose) { logger.debug("findNode {} {}", inputThatTopic(input, that, topic), result); }
        return result;
    }

    public static boolean verbose = false;

    /**
     * Recursively find a leaf node given a starting node and a path.
     *
     * @param node string node
     * @param path string path
     * @return the leaf node or null if no leaf is found
     */
    Nodemapper findNode(Nodemapper node, Path path) {
        if (path == null && node != null) {
            if (verbose) {
                logger.debug("findNode: path is null, returning node {}", node.category.inputThatTopic());
            }
            return node;
        } else if ("<THAT> * <TOPIC> *".equals(Path.pathToSentence(path).trim()) && node.shortCut && "<THAT>".equals(path.word)) {
            if (verbose) { logger.debug("findNode: shortcut, returning {}", node.category.inputThatTopic()); }
            return node;
        } else if (node.containsKey(path.word)) {
            if (verbose) { logger.debug("findNode: node contains {}", path.word); }
            Nodemapper nextNode = node.get(path.word.toUpperCase());
            return findNode(nextNode, path.next);
        } else {
            if (verbose) { logger.debug("findNode: returning null"); }
            return null;
        }
    }

    /**
     * Find the matching leaf node given an input, that state and topic value
     *
     * @param input client input
     * @param that  bot's last sentence
     * @param topic current topic
     * @return matching leaf node or null if no match is found
     */
    public final Nodemapper match(String input, String that, String topic) {
        Nodemapper n = null;
        try {
            String inputThatTopic = inputThatTopic(input, that, topic);
            //System.out.println("Matching: "+inputThatTopic);
            Path p = Path.sentenceToPath(inputThatTopic);
            //p.print();
            n = match(p, inputThatTopic);
            if (logger.isDebugEnabled()) {
                if (n != null) {
                    //MagicBooleans.trace("in graphmaster.match(), matched "+n.category.inputThatTopic()+" "+n.category.getFilename());
                    logger.debug("Matched: {} {}", n.category.inputThatTopic(), n.category.getFilename());
                } else {
                    //MagicBooleans.trace("in graphmaster.match(), no match.");
                    logger.debug("No match.");
                }

            }
        } catch (Exception ex) {
            logger.error("match error", ex);
            n = null;
        }
        if (logger.isDebugEnabled() && Chat.matchTrace.length() < MagicNumbers.max_trace_length) {
            if (n != null) {
                Chat.setMatchTrace(Chat.matchTrace + n.category.inputThatTopic() + "\n");
            }
        }
        //MagicBooleans.trace("in graphmaster.match(), returning: " + n);
        return n;
    }

    /**
     * Find the matching leaf node given a path of the form "{@code input <THAT> that <TOPIC> topic}"
     *
     * @return matching leaf node or null if no match is found
     */
    final Nodemapper match(Path path, String inputThatTopic) {
        try {
            String[] inputStars = new String[MagicNumbers.max_stars];
            String[] thatStars = new String[MagicNumbers.max_stars];
            String[] topicStars = new String[MagicNumbers.max_stars];
            String starState = "inputStar";
            String matchTrace = "";
            Nodemapper n = match(path, root, inputThatTopic, starState, 0, inputStars, thatStars, topicStars, matchTrace);
            if (n != null) {
                StarBindings sb = new StarBindings();
                for (int i = 0; inputStars[i] != null && i < MagicNumbers.max_stars; i++) {
                    sb.inputStars.add(inputStars[i]);
                }
                for (int i = 0; thatStars[i] != null && i < MagicNumbers.max_stars; i++) {
                    sb.thatStars.add(thatStars[i]);
                }
                for (int i = 0; topicStars[i] != null && i < MagicNumbers.max_stars; i++) {
                    sb.topicStars.add(topicStars[i]);
                }
                n.starBindings = sb;
            }
            //if (!n.category.getPattern().contains("*")) System.out.println("adding match "+inputThatTopic);
            if (n != null) { n.category.addMatch(inputThatTopic); }
            return n;
        } catch (Exception ex) {
            logger.error("match error", ex);
            return null;
        }
    }

    /**
     * Depth-first search of the graph for a matching leaf node.
     * At each node, the order of search is
     * 1. $WORD  (high priority exact word match)
     * 2. # wildcard  (zero or more word match)
     * 3. _ wildcard (one or more words match)
     * 4. WORD (exact word match)
     * 5. {@code <set></set>} (AIML Set match)
     * 6. shortcut (graph shortcut when that pattern = * and topic pattern = *)
     * 7. ^ wildcard  (zero or more words match)
     * 8. * wildcard (one or more words match)
     *
     * @param path           remaining path to be matched
     * @param node           current search node
     * @param inputThatTopic original input, that and topic string
     * @param starState      tells whether wildcards are in input pattern, that pattern or topic pattern
     * @param starIndex      index of wildcard
     * @param inputStars     array of input pattern wildcard matches
     * @param thatStars      array of that pattern wildcard matches
     * @param topicStars     array of topic pattern wildcard matches
     * @param matchTrace     trace of match path for debugging purposes
     * @return matching leaf node or null if no match is found
     */
    final Nodemapper match(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        Nodemapper matchedNode;
        //System.out.println("Match: Height="+node.height+" Length="+path.length+" Path="+Path.pathToSentence(path));
        matchCount++;
        if ((matchedNode = nullMatch(path, node, matchTrace)) != null) {
            return matchedNode;
        } else if (path.length < node.height) {
            return null;
        } else if ((matchedNode = dollarMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = sharpMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = underMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = wordMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = setMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = shortCutMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = caretMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else if ((matchedNode = starMatch(path, node, inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else {
            return null;
        }
    }

    /**
     * print out match trace when search fails
     *
     * @param mode  Which mode of search
     * @param trace Match trace info
     */
    void fail(String mode, String trace) {
        // System.out.println("Match failed ("+mode+") "+trace);
    }

    /**
     * a match is found if the end of the path is reached and the node is a leaf node
     *
     * @param path       remaining path
     * @param node       current search node
     * @param matchTrace trace of match for debugging purposes
     * @return matching leaf node or null if no match found
     */
    final Nodemapper nullMatch(Path path, Nodemapper node, String matchTrace) {
        if (path == null && node != null && node.isLeaf() && node.category != null) {
            return node;
        } else {
            fail("null", matchTrace);
            return null;
        }
    }

    final Nodemapper shortCutMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        if (node != null && node.shortCut && "<THAT>".equals(path.word) && node.category != null) {
            String tail = Path.pathToSentence(path).trim();
            //System.out.println("Shortcut tail = "+tail);
            String that = tail.substring(tail.indexOf("<THAT>") + "<THAT>".length(), tail.indexOf("<TOPIC>")).trim();
            String topic = tail.substring(tail.indexOf("<TOPIC>") + "<TOPIC>".length(), tail.length()).trim();
            //System.out.println("Shortcut that = "+that+" topic = "+topic);
            //System.out.println("Shortcut matched: "+node.category.inputThatTopic());
            thatStars[0] = that;
            topicStars[0] = topic;
            return node;
        } else {
            fail("shortCut", matchTrace);
            return null;
        }
    }

    final Nodemapper wordMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        try {
            String uword = path.word.toUpperCase();
            if ("<THAT>".equals(uword)) {
                starIndex = 0;
                starState = "thatStar";
            } else if ("<TOPIC>".equals(uword)) {
                starIndex = 0;
                starState = "topicStar";
            }
            //System.out.println("path.next= "+path.next+" node.get="+node.get(uword));
            matchTrace += "[" + uword + "," + uword + "]";
            Nodemapper matchedNode;
            if (path != null && node.containsKey(uword) &&
                (matchedNode = match(path.next, node.get(uword), inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
                return matchedNode;
            } else {
                fail("word", matchTrace);
                return null;
            }
        } catch (Exception ex) {
            logger.error("wordMatch error: {}", Path.pathToSentence(path), ex);
            return null;
        }
    }

    final Nodemapper dollarMatch(Path path, Nodemapper node, String inputThatTopic, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        String uword = "$" + path.word.toUpperCase();
        Nodemapper matchedNode;
        if (path != null && node.containsKey(uword) && (matchedNode = match(path.next, node.get(uword), inputThatTopic, starState, starIndex, inputStars, thatStars, topicStars, matchTrace)) != null) {
            return matchedNode;
        } else {
            fail("dollar", matchTrace);
            return null;
        }
    }

    final Nodemapper starMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "*", matchTrace);
    }

    final Nodemapper underMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "_", matchTrace);
    }

    final Nodemapper caretMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        Nodemapper matchedNode = zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^", matchTrace);
        if (matchedNode != null) {
            return matchedNode;
        } else {
            return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "^", matchTrace);
        }
    }

    final Nodemapper sharpMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        Nodemapper matchedNode = zeroMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#", matchTrace);
        if (matchedNode != null) {
            return matchedNode;
        } else {
            return wildMatch(path, node, input, starState, starIndex, inputStars, thatStars, topicStars, "#", matchTrace);
        }
    }

    final Nodemapper zeroMatch(Path path, Nodemapper node, String input, String starState, int starIndex,
                               String[] inputStars, String[] thatStars, String[] topicStars, String wildcard, String matchTrace) {
        // System.out.println("Entering zeroMatch on "+path.word+" "+node.get(wildcard));
        matchTrace += "[" + wildcard + ",]";
        if (path != null && node.containsKey(wildcard)) {
            //System.out.println("Zero match calling setStars Prop "+MagicStrings.null_star+" = "+bot.properties.get(MagicStrings.null_star));
            setStars(bot.properties.get(MagicStrings.null_star), starIndex, starState, inputStars, thatStars, topicStars);
            Nodemapper nextNode = node.get(wildcard);
            return match(path, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace);
        } else {
            fail("zero " + wildcard, matchTrace);
            return null;
        }

    }

    final Nodemapper wildMatch(Path path, Nodemapper node, String input, String starState, int starIndex,
                               String[] inputStars, String[] thatStars, String[] topicStars, String wildcard, String matchTrace) {
        if ("<THAT>".equals(path.word) || "<TOPIC>".equals(path.word)) {
            fail("wild1 " + wildcard, matchTrace);
            return null;
        }
        try {
            if (path != null && node.containsKey(wildcard)) {
                matchTrace += "[" + wildcard + "," + path.word + "]";
                String currentWord = path.word;
                String starWords = currentWord + " ";
                Path pathStart = path.next;
                Nodemapper nextNode = node.get(wildcard);
                Nodemapper matchedNode;
                if (nextNode.isLeaf() && !nextNode.shortCut) {
                    matchedNode = nextNode;
                    starWords = Path.pathToSentence(path);
                    //System.out.println(starIndex+". starwords="+starWords);
                    setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
                    return matchedNode;
                } else {
                    for (path = pathStart; path != null && !"<THAT>".equals(currentWord) && !"<TOPIC>".equals(currentWord); path = path.next) {
                        matchTrace += "[" + wildcard + "," + path.word + "]";
                        if ((matchedNode = match(path, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
                            setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
                            return matchedNode;
                        } else {
                            currentWord = path.word;
                            starWords += currentWord + " ";
                        }
                    }
                    fail("wild2 " + wildcard, matchTrace);
                    return null;
                }
            }
        } catch (Exception ex) {
            logger.error("wildMatch error: {}", Path.pathToSentence(path), ex);
        }
        fail("wild3 " + wildcard, matchTrace);
        return null;
    }

    final Nodemapper setMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        logger.debug("Graphmaster.setMatch(path: {}, node: {}, input: {}, starState: {}, starIndex: {}, inputStars, thatStars, topicStars, matchTrace: {}, )",
            path, node, input, starState, starIndex, matchTrace);
        if (node.sets == null || "<THAT>".equals(path.word) || "<TOPIC>".equals(path.word)) { return null; }
        logger.debug("in Graphmaster.setMatch, setMatch sets ={}", node.sets);
        for (String setName : node.sets) {
            logger.debug("in Graphmaster.setMatch, setMatch trying type {}", setName);
            Nodemapper nextNode = node.get("<SET>" + setName.toUpperCase() + "</SET>");
            AIMLSet aimlSet = bot.setMap.get(setName);
            //System.out.println(aimlSet.name + "="+ aimlSet);
            String currentWord = path.word;
            String starWords = currentWord + " ";
            int length = 1;
            matchTrace += "[<set>" + setName + "</set>," + path.word + "]";
            logger.debug("in Graphmaster.setMatch, setMatch starWords =\"{}\"", starWords);
            Nodemapper bestMatchedNode = null;
            for (Path qath = path.next; qath != null && !"<THAT>".equals(currentWord) && !"<TOPIC>".equals(currentWord) && length <= aimlSet.maxLength(); qath = qath.next) {
                logger.debug("in Graphmaster.setMatch, qath.word = {}", qath.word);
                String phrase = bot.preProcessor.normalize(starWords.trim()).toUpperCase();
                logger.debug("in Graphmaster.setMatch, setMatch trying \"{}\" in {}", phrase, setName);
                Nodemapper matchedNode;
                if (aimlSet.contains(phrase) && (matchedNode = match(qath, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
                    setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
                    logger.debug("in Graphmaster.setMatch, setMatch found {} in {}", phrase, setName);
                    bestMatchedNode = matchedNode;
                }
                //    else if (qath.word.equals("<THAT>") || qath.word.equals("<TOPIC>")) return null;

                length += 1;
                currentWord = qath.word;
                starWords += currentWord + " ";

            }
            if (bestMatchedNode != null) { return bestMatchedNode; }
        }
        fail("set", matchTrace);
        return null;
    }

    /*
    final Nodemapper setMatch(Path path, Nodemapper node, String input, String starState, int starIndex, String[] inputStars, String[] thatStars, String[] topicStars, String matchTrace) {
        if (DEBUG) System.out.println("Graphmaster.setMatch(path: " + path + ", node: " + node + ", input: " + input + ", starState: " + starState + ", starIndex: " + starIndex + ", inputStars, thatStars, topicStars, matchTrace: " + matchTrace + ", )");
        if (node.sets == null || path.word.equals("<THAT>") || path.word.equals("<TOPIC>")) return null;
        if (DEBUG) System.out.println("in Graphmaster.setMatch, setMatch sets ="+node.sets);
        for (String name : node.sets) {
            if (DEBUG) System.out.println("in Graphmaster.setMatch, setMatch trying type "+name);
            Nodemapper nextNode = node.get("<SET>"+name.toUpperCase()+"</SET>");
            AIMLSet aimlSet = bot.setMap.get(name);
            //System.out.println(aimlSet.name + "="+ aimlSet);
            Nodemapper matchedNode;
            String currentWord = path.word;
            String starWords = currentWord+" ";
            int length = 1;
            matchTrace += "[<set>"+name+"</set>,"+path.word+"]";
            if (DEBUG) System.out.println("in Graphmaster.setMatch, setMatch starWords =\""+starWords+"\"");
            for (Path qath = path.next; qath != null &&  !currentWord.equals("<THAT>") && !currentWord.equals("<TOPIC>") && length <= aimlSet.maxLength; qath = qath.next) {
                if (DEBUG) System.out.println("in Graphmaster.setMatch, qath.word = "+qath.word);
                String phrase = bot.preProcessor.normalize(starWords.trim()).toUpperCase();
                if (DEBUG) System.out.println("in Graphmaster.setMatch, setMatch trying \""+phrase+"\" in "+name);
                if (aimlSet.contains(phrase) && (matchedNode = match(qath, nextNode, input, starState, starIndex + 1, inputStars, thatStars, topicStars, matchTrace)) != null) {
                    setStars(starWords, starIndex, starState, inputStars, thatStars, topicStars);
                    if (DEBUG) System.out.println("in Graphmaster.setMatch, setMatch found "+phrase+" in "+ name);
                    return matchedNode;
                }
                //    else if (qath.word.equals("<THAT>") || qath.word.equals("<TOPIC>")) return null;
                else {
                    length = length + 1;
                    currentWord = qath.word;
                    starWords += currentWord + " ";
                }
            }
        }
        fail("set", matchTrace);
        return null;
    }*/

    public void setStars(String starWords, int starIndex, String starState, String[] inputStars, String[] thatStars, String[] topicStars) {
        if (starIndex < MagicNumbers.max_stars) {
            //System.out.println("starWords="+starWords);
            starWords = starWords.trim();
            if ("inputStar".equals(starState)) {
                inputStars[starIndex] = starWords;
            } else if ("thatStar".equals(starState)) {
                thatStars[starIndex] = starWords;
            } else if ("topicStar".equals(starState)) {
                topicStars[starIndex] = starWords;
            }
        }
    }

    public void printgraph() {
        printgraph(root, "");
    }

    void printgraph(Nodemapper node, String partial) {
        if (node == null) {
            logger.info("Null graph");
        } else {
            if (node.isLeaf() || node.shortCut) {
                String template = Category.templateToLine(node.category.getTemplate());
                template = template.substring(0, Math.min(16, template.length()));
                if (node.shortCut) {
                    logger.info("{}({}[{}])--<THAT>-->X(1)--*-->X(1)--<TOPIC>-->X(1)--*-->{}...",
                        partial, node.size(), node.height, template);
                } else {
                    logger.info("{}({}[{}]) {}...", partial, node.size(), node.height, template);
                }
            }
            for (String key : node.keySet()) {
                //System.out.println(key);
                printgraph(node.get(key), partial + "(" + node.size() + "[" + node.height + "])--" + key + "-->");
            }
        }
    }

    public ArrayList<Category> getCategories() {
        ArrayList<Category> categories = new ArrayList<>();
        getCategories(root, categories);
        //for (Category c : categories) System.out.println("getCategories: "+c.inputThatTopic()+" "+c.getTemplate());
        return categories;
    }

    void getCategories(Nodemapper node, ArrayList<Category> categories) {
        if (node == null) {
            return;
        }
        if (node.isLeaf() || node.shortCut) {
            if (node.category != null) {
                categories.add(node.category);   // node.category == null when the category is deleted.
            }
        }
        for (String key : node.keySet()) {
            //System.out.println(key);
            getCategories(node.get(key), categories);
        }
    }

    int leafCnt;
    int nodeCnt;
    long nodeSize;
    int singletonCnt;
    int shortCutCnt;
    int naryCnt;

    public void nodeStats() {
        leafCnt = 0;
        nodeCnt = 0;
        nodeSize = 0;
        singletonCnt = 0;
        shortCutCnt = 0;
        naryCnt = 0;
        nodeStatsGraph(root);
        resultNote = bot.name + " (" + name + "): " + getCategories().size() + " categories " + nodeCnt + " nodes " + singletonCnt + " singletons " + leafCnt + " leaves " + shortCutCnt + " shortcuts " + naryCnt + " n-ary " + nodeSize + " branches " + (float) nodeSize / (float) nodeCnt + " average branching ";
        logger.debug(resultNote);
    }

    public void nodeStatsGraph(Nodemapper node) {
        if (node != null) {
            //System.out.println("Counting "+node.key+ " size="+node.size());
            nodeCnt++;
            nodeSize += node.size();
            if (node.size() == 1) { singletonCnt += 1; }
            if (node.isLeaf() && !node.shortCut) {
                leafCnt++;
            }
            if (node.size() > 1) { naryCnt += 1; }
            if (node.shortCut) {shortCutCnt += 1;}
            for (String key : node.keySet()) {
                nodeStatsGraph(node.get(key));
            }
        }
    }

    public Set<String> getVocabulary() {
        vocabulary = new HashSet<>();
        getBrainVocabulary(root);
        for (String set : bot.setMap.keySet()) { vocabulary.addAll(bot.setMap.get(set).values()); }
        return vocabulary;
    }

    public void getBrainVocabulary(Nodemapper node) {
        if (node != null) {
            //System.out.println("Counting "+node.key+ " size="+node.size());
            for (String key : node.keySet()) {
                vocabulary.add(key);
                getBrainVocabulary(node.get(key));
            }
        }
    }
}
