package org.alicebot.ab.utils;

import net.reduls.sanmoku.Morpheme;
import net.reduls.sanmoku.Tagger;
import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicStrings;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class JapaneseUtils {

    private JapaneseUtils() {}

    /**
     * Tokenize a fragment of the input that contains only text
     *
     * @param fragment fragment of input containing only text and no XML tags
     * @return tokenized fragment
     */
    public static String tokenizeFragment(String fragment) {
        //System.out.println("buildFragment "+fragment);
        StringBuilder result = new StringBuilder();
        for (Morpheme e : Tagger.parse(fragment)) {
            result.append(e.surface).append(" ");
            //
            // System.out.println("Feature "+e.feature+" Surface="+e.surface);
        }
        return result.toString().trim();
    }

    /**
     * Morphological analysis of an input sentence that contains an AIML pattern.
     *
     * @return morphed sentence with one space between words, preserving XML markup and AIML $ operation
     */
    public static String tokenizeSentence(String sentence) {
        //System.out.println("tokenizeSentence "+sentence);
        if (!MagicBooleans.jp_tokenize) { return sentence; }
        String result = tokenizeXML(sentence);
        while (result.contains("$ ")) { result = result.replace("$ ", "$"); }
        while (result.contains("  ")) { result = result.replace("  ", " "); }
        while (result.contains("anon ")) {
            result = result.replace("anon ", "anon"); // for Triple Store
        }
        result = result.trim();
        //if (MagicBooleans.trace_mode) System.out.println("tokenizeSentence '"+sentence+"'-->'"+result+"'");
        return result;
    }

    public static String tokenizeXML(String xmlExpression) {
        //System.out.println("tokenizeXML "+xmlExpression);
        String response = MagicStrings.template_failed;
        try {
            xmlExpression = "<sentence>" + xmlExpression + "</sentence>";
            Node root = DomUtils.parseString(xmlExpression);
            response = recursEval(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return AIMLProcessor.trimTag(response, "sentence");
    }

    private static String recursEval(Node node) {
        try {

            String nodeName = node.getNodeName();
            //System.out.println("recursEval "+nodeName);
            switch (nodeName) {
                case "#text":
                    return tokenizeFragment(node.getNodeValue());
                case "sentence":
                    return evalTagContent(node);
                default:
                    return (genericXML(node));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "JP Morph Error";
    }

    public static String genericXML(Node node) {
        //System.out.println("genericXML "+node.getNodeName());
        String result = evalTagContent(node);
        return unevaluatedXML(result, node);
    }

    public static String evalTagContent(Node node) {
        StringBuilder result = new StringBuilder();
        //System.out.println("evalTagContent "+node.getNodeName());
        try {
            NodeList childList = node.getChildNodes();
            for (int i = 0; i < childList.getLength(); i++) {
                Node child = childList.item(i);
                result.append(recursEval(child));
            }
        } catch (Exception ex) {
            System.out.println("Something went wrong with evalTagContent");
            ex.printStackTrace();
        }
        return result.toString();
    }

    private static String unevaluatedXML(String result, Node node) {
        String nodeName = node.getNodeName();
        StringBuilder attributesBuilder = new StringBuilder();
        if (node.hasAttributes()) {
            NamedNodeMap XMLAttributes = node.getAttributes();
            for (int i = 0; i < XMLAttributes.getLength(); i++)

            {
                attributesBuilder.append(" ")
                    .append(XMLAttributes.item(i).getNodeName())
                    .append("=\"")
                    .append(XMLAttributes.item(i).getNodeValue())
                    .append("\"");
            }
        }
        String attributes = attributesBuilder.toString();
        if (result.isEmpty()) {
            return " <" + nodeName + attributes + "/> ";
        } else {
            return " <" + nodeName + attributes + ">" + result + "</" + nodeName + "> ";   // add spaces
        }
    }
}
