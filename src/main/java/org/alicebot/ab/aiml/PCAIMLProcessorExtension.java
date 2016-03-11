package org.alicebot.ab.aiml;
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

import org.alicebot.ab.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This is just a stub to make the contactaction.aiml file work on a PC
 * with some extension tags that are defined for mobile devices.
 */
public class PCAIMLProcessorExtension implements AIMLProcessorExtension {

    private static final Logger logger = LoggerFactory.getLogger(PCAIMLProcessorExtension.class);

    @Override
    public boolean canProcessTag(String tagName) {
        return Stream.of(TagProcessor.values()).anyMatch(e -> e.canProcessTag(tagName));
    }

    private enum TagProcessor implements AIMLProcessorExtension {
        NEW_CONTACT("addinfo") {
            @Override
            public String recursEval(Node node, Function<Node, String> evalTagContent) {
                NodeList childList = node.getChildNodes();
                String emailAddress = "unknown";
                String displayName = "unknown";
                String dialNumber = "unknown";
                String emailType = "unknown";
                String phoneType = "unknown";
                String birthday = "unknown";
                for (int i = 0; i < childList.getLength(); i++) {
                    if ("birthday".equals(childList.item(i).getNodeName())) {
                        birthday = evalTagContent.apply(childList.item(i));
                    }
                    if ("phonetype".equals(childList.item(i).getNodeName())) {
                        phoneType = evalTagContent.apply(childList.item(i));
                    }
                    if ("emailtype".equals(childList.item(i).getNodeName())) {
                        emailType = evalTagContent.apply(childList.item(i));
                    }
                    if ("dialnumber".equals(childList.item(i).getNodeName())) {
                        dialNumber = evalTagContent.apply(childList.item(i));
                    }
                    if ("displayname".equals(childList.item(i).getNodeName())) {
                        displayName = evalTagContent.apply(childList.item(i));
                    }
                    if ("emailaddress".equals(childList.item(i).getNodeName())) {
                        emailAddress = evalTagContent.apply(childList.item(i));
                    }
                }
                logger.info("Adding new contact {} {} {} {} {} {}",
                    displayName, phoneType, dialNumber, emailType, emailAddress, birthday);
                // the contact adds itself to the contact list
                new Contact(displayName, phoneType, dialNumber, emailType, emailAddress, birthday);
                return "";
            }
        },
        CONTACT_ID("contactid") {
            @Override
            public String recursEval(Node node, Function<Node, String> evalTagContent) {
                String displayName = evalTagContent.apply(node);
                return Contact.contactId(displayName);
            }
        },
        MULTIPLE_IDS("multipleids") {
            @Override
            public String recursEval(Node node, Function<Node, String> evalTagContent) {
                String contactName = evalTagContent.apply(node);
                return Contact.multipleIds(contactName);
            }
        },
        DISPLAY_NAME("displayname") {
            @Override
            public String recursEval(Node node, Function<Node, String> evalTagContent) {
                String id = evalTagContent.apply(node);
                return Contact.displayName(id);
            }
        },
        DIAL_NUMBER("dialnumber") {
            @Override
            public String recursEval(Node node, Function<Node, String> evalTagContent) {
                NodeList childList = node.getChildNodes();
                String id = "unknown";
                String type = "unknown";
                for (int i = 0; i < childList.getLength(); i++) {
                    if ("id".equals(childList.item(i).getNodeName())) {
                        id = evalTagContent.apply(childList.item(i));
                    }
                    if ("type".equals(childList.item(i).getNodeName())) {
                        type = evalTagContent.apply(childList.item(i));
                    }
                }
                return Contact.dialNumber(type, id);
            }
        },
        EMAIL_ADDRESS("emailaddress") {
            @Override
            public String recursEval(Node node, Function<Node, String> evalTagContent) {
                NodeList childList = node.getChildNodes();
                String id = "unknown";
                String type = "unknown";
                for (int i = 0; i < childList.getLength(); i++) {
                    if ("id".equals(childList.item(i).getNodeName())) {
                        id = evalTagContent.apply(childList.item(i));
                    }
                    if ("type".equals(childList.item(i).getNodeName())) {
                        type = evalTagContent.apply(childList.item(i));
                    }
                }
                return Contact.emailAddress(type, id);
            }
        },
        BIRTHDAY("contactbirthday") {
            @Override
            public String recursEval(Node node, Function<Node, String> evalTagContent) {
                String id = evalTagContent.apply(node);
                return Contact.birthday(id);
            }
        };

        private final String tagName;

        TagProcessor(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public boolean canProcessTag(String tagName) {
            return this.tagName.equals(tagName);
        }

    }

    @Override
    public String recursEval(Node node, Function<Node, String> evalTagContent) {
        try {
            String nodeName = node.getNodeName();
            return Stream.of(TagProcessor.values())
                .filter(e -> e.canProcessTag(nodeName)).findFirst()
                .map(e -> e.recursEval(node, evalTagContent))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported tag " + node.getNodeName()));
        } catch (Exception ex) {
            logger.error("recursEval error", ex);
            return "";
        }
    }
}
