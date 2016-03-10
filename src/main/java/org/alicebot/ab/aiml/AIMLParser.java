package org.alicebot.ab.aiml;

import org.alicebot.ab.Category;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.utils.DomUtils;
import org.alicebot.ab.utils.JapaneseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AIMLParser {

    private static final Logger logger = LoggerFactory.getLogger(AIMLParser.class);

    /**
     * convert an AIML file to a list of categories.
     *
     * @param directory directory containing the AIML file.
     * @param aimlFile  AIML file name.
     * @return list of categories.
     */
    public List<Category> AIMLToCategories(Path directory, String aimlFile) throws SAXException, IOException, ParserConfigurationException {
        List<Category> categories = new ArrayList<>();
        Node root = DomUtils.parseFile(directory.resolve(aimlFile).toFile());      // <aiml> tag
        NodeList nodelist = root.getChildNodes();
        for (int i = 0; i < nodelist.getLength(); i++) {
            Node n = nodelist.item(i);
            //System.out.println("AIML child: " +n.getNodeName());
            if ("category".equals(n.getNodeName())) {
                categoryProcessor(n, "*", aimlFile).ifPresent(categories::add);
            } else if ("topic".equals(n.getNodeName())) {
                String topic = n.getAttributes().getNamedItem("name").getTextContent();
                //System.out.println("topic: " + topic);
                NodeList children = n.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node m = children.item(j);
                    //System.out.println("Topic child: " + m.getNodeName());
                    if ("category".equals(m.getNodeName())) {
                        categoryProcessor(m, topic, aimlFile).ifPresent(categories::add);
                    }
                }
            }
        }
        return categories;
    }

    /**
     * when parsing an AIML file, process a category element.
     *
     * @param n        current XML parse node.
     * @param topic    value of topic in case this category is wrapped in a <topic> tag
     * @param aimlFile name of AIML file being parsed.
     */
    private Optional<Category> categoryProcessor(Node n, String topic, String aimlFile) {

        NodeList children = n.getChildNodes();
        String pattern = "*";
        String that = "*";
        String template = "";
        for (int j = 0; j < children.getLength(); j++) {
            //System.out.println("CHILD: " + children.item(j).getNodeName());
            Node m = children.item(j);
            String mName = m.getNodeName();
            //System.out.println("mName: " + mName);
            if ("#text".equals(mName)) {
                /*skip*/
            } else if ("pattern".equals(mName)) {
                pattern = DomUtils.nodeToString(m);
            } else if ("that".equals(mName)) {
                that = DomUtils.nodeToString(m);
            } else if ("topic".equals(mName)) {
                topic = DomUtils.nodeToString(m);
            } else if ("template".equals(mName)) {
                template = DomUtils.nodeToString(m);
            } else {
                logger.info("categoryProcessor: unexpected {} in {}", mName, DomUtils.nodeToString(m));
            }
        }
        //System.out.println("categoryProcessor: pattern="+pattern);
        pattern = trimTag(pattern, "pattern");
        that = trimTag(that, "that");
        topic = trimTag(topic, "topic");
        pattern = cleanPattern(pattern);
        that = cleanPattern(that);
        topic = cleanPattern(topic);

        template = trimTag(template, "template");
        if (MagicBooleans.jp_tokenize) {
            pattern = JapaneseUtils.tokenizeSentence(pattern);
            that = JapaneseUtils.tokenizeSentence(that);
            topic = JapaneseUtils.tokenizeSentence(topic);
        }
        Category c = new Category(0, pattern, that, topic, template, aimlFile);
        /*if (template == null) System.out.println("Template is null");
        if (template.length()==0) System.out.println("Template is zero length");*/
        if (template.isEmpty()) {
            logger.info("Category {} discarded due to blank or missing <template>.", c.inputThatTopic());
        } else {
            return Optional.of(c);
        }
        return Optional.empty();
    }

    private String cleanPattern(String pattern) {
        pattern = pattern.replaceAll("(\r\n|\n\r|\r|\n)", " ");
        pattern = pattern.replaceAll("  ", " ");
        return pattern.trim();
    }

    public static String trimTag(String s, String tagName) {
        String stag = "<" + tagName + ">";
        String etag = "</" + tagName + ">";
        if (s.startsWith(stag) && s.endsWith(etag)) {
            s = s.substring(stag.length());
            s = s.substring(0, s.length() - etag.length());
        }
        return s.trim();
    }

}
