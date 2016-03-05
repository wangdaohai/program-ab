package org.alicebot.ab.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class DomUtils {

    private static final Logger logger = LoggerFactory.getLogger(DomUtils.class);

    private DomUtils() {}

    public static Node parseFile(String fileName) throws SAXException, IOException, ParserConfigurationException {
        File file = new File(fileName);
        return parseDoc(documentBuilder().parse(file));
    }

    public static Node parseString(String string) throws SAXException, IOException, ParserConfigurationException {
        InputStream is = new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_16));
        return parseDoc(documentBuilder().parse(is));
    }

    private static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        // from AIMLProcessor.evalTemplate and AIMLProcessor.validTemplate:
        //   dbFactory.setIgnoringComments(true); // fix this
        return dbFactory.newDocumentBuilder();
    }

    private static Node parseDoc(Document doc) {
        doc.getDocumentElement().normalize();
        return doc.getDocumentElement();
    }

    /**
     * convert an XML node to an XML statement
     *
     * @param node current XML node
     * @return XML string
     */
    public static String nodeToString(Node node) {
        //MagicBooleans.trace("nodeToString(node: " + node + ")");
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            logger.error("nodeToString Transformer Exception", te);
        }
        return sw.toString();
    }
}
