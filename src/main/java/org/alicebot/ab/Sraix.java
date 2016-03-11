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

import okhttp3.HttpUrl;
import org.alicebot.ab.aiml.AIMLProcessor;
import org.alicebot.ab.utils.CalendarUtils;
import org.alicebot.ab.utils.IOUtils;
import org.alicebot.ab.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Sraix {

    private static final Logger logger = LoggerFactory.getLogger(Sraix.class);
    private static final String SRAIX_FAILED = "SRAIXFAILED";
    private static final String SHOPPING_HINT = "shopping";
    private static final String PIC_HINT = "pic";
    private static final String EVENT_HINT = "event";
    private static final String NO_HINT = "nohint";

    private static final NetworkUtils network = new NetworkUtils();

    private Sraix() {}

    private static Map<String, String> custIdMap = new HashMap<>();

    private static String custid = "1"; // customer ID number for Pandorabots

    public static String sraix(Chat chatSession, String input, String defaultResponse, String hint, String host, String botid, String apiKey, String limit) {
        String response;
        if (!MagicBooleans.enable_network_connection) {
            response = SRAIX_FAILED;
        } else if (host != null && botid != null) {
            response = sraixPandorabots(input, chatSession, host, botid);
        } else {
            response = sraixPannous(input, hint, chatSession);
        }
        logger.debug("Sraix: response = {} defaultResponse = {}", response, defaultResponse);
        if (response.equals(SRAIX_FAILED)) {
            if (chatSession != null && defaultResponse == null) {
                response = new AIMLProcessor(chatSession).respond(SRAIX_FAILED, "nothing", "nothing");
            } else if (defaultResponse != null) {
                response = defaultResponse;
            }
        }
        return response;
    }

    private static String sraixPandorabots(String input, Chat chatSession, String host, String botid) {
        //System.out.println("Entering SRAIX with input="+input+" host ="+host+" botid="+botid);
        String responseContent = pandorabotsRequest(input, host, botid);
        if (responseContent == null) {
            return SRAIX_FAILED;
        } else {
            return pandorabotsResponse(responseContent, chatSession, host, botid);
        }
    }

    private static String pandorabotsRequest(String input, String host, String botid) {
        try {
            custid = "0";
            String key = host + ":" + botid;
            if (custIdMap.containsKey(key)) { custid = custIdMap.get(key); }
            HttpUrl spec = network.pandoraBotUrl(host, botid, custid, input);
            logger.debug("Spec = {}", spec);
            return network.responseContent(spec);
        } catch (Exception ex) {
            logger.error("pandorabotsRequest error", ex);
            return null;
        }
    }

    private static String pandorabotsResponse(String sraixResponse, Chat chatSession, String host, String botid) {
        String botResponse = SRAIX_FAILED;
        try {
            int n1 = sraixResponse.indexOf("<that>");
            int n2 = sraixResponse.indexOf("</that>");

            if (n2 > n1) { botResponse = sraixResponse.substring(n1 + "<that>".length(), n2); }
            n1 = sraixResponse.indexOf("custid=");
            if (n1 > 0) {
                custid = sraixResponse.substring(n1 + "custid=\"".length(), sraixResponse.length());
                n2 = custid.indexOf('\"');
                custid = n2 > 0 ? custid.substring(0, n2) : "0";
                String key = host + ":" + botid;
                //System.out.println("--> Map "+key+" --> "+custid);
                custIdMap.put(key, custid);
            }
            if (botResponse.endsWith(".")) {
                botResponse = botResponse.substring(0, botResponse.length() - 1);   // snnoying Pandorabots extra "."
            }
        } catch (Exception ex) {
            logger.error("pandorabotsResponse error", ex);
        }
        return botResponse;
    }

    private static String sraixPannous(String input, String hint, Chat chatSession) {
        try {
            if (hint == null) { hint = NO_HINT; }
            HttpUrl.Builder builder = new HttpUrl.Builder()
                .scheme("https").host("ask.pannous.com").addPathSegment("api")
                .addQueryParameter("input", pannousCleanInput(input)) // chatSession.bot.preProcessor.denormalize(input)
                .addQueryParameter("locale", "en_US")
                .addQueryParameter("timezone", String.valueOf(CalendarUtils.timeZoneOffset()))
                .addQueryParameter("login", MagicStrings.pannous_login)
                .addQueryParameter("ip", network.localIPAddress())
                .addQueryParameter("botid", "0")
                .addQueryParameter("key", MagicStrings.pannous_api_key)
                .addQueryParameter("exclude", "Dialogues,ChatBot")
                .addQueryParameter("out", "json")
                .addQueryParameter("clientFeatures", "show-images,reminder,say")
                .addQueryParameter("debug", "true");
            if (Chat.locationKnown) {
                builder.addQueryParameter("location", Chat.latitude + "," + Chat.longitude);
            }

            HttpUrl url = builder.build();
            logger.debug("in Sraix.sraixPannous, url: {}", url);
            String page = network.responseContent(url);
            String text = "";
            if (page == null || page.isEmpty()) {
                text = SRAIX_FAILED;
            } else {
                JSONArray outputJson = new JSONObject(page).getJSONArray("output");
                //MagicBooleans.trace("in Sraix.sraixPannous, outputJson class: " + outputJson.getClass() + ", outputJson: " + outputJson);
                String imgRef = "";
                String urlRef = "";
                if (outputJson.length() == 0) {
                    text = SRAIX_FAILED;
                } else {
                    JSONObject firstHandler = outputJson.getJSONObject(0);
                    //MagicBooleans.trace("in Sraix.sraixPannous, firstHandler class: " + firstHandler.getClass() + ", firstHandler: " + firstHandler);
                    JSONObject actions = firstHandler.getJSONObject("actions");
                    //MagicBooleans.trace("in Sraix.sraixPannous, actions class: " + actions.getClass() + ", actions: " + actions);
                    if (actions.has("reminder")) {
                        //MagicBooleans.trace("in Sraix.sraixPannous, found reminder action");
                        Object obj = actions.get("reminder");
                        if (obj instanceof JSONObject) {
                            logger.debug("Found JSON Object");
                            JSONObject sObj = (JSONObject) obj;
                            String date = sObj.getString("date");
                            date = date.substring(0, "2012-10-24T14:32".length());
                            logger.debug("date={}", date);
                            String duration = sObj.getString("duration");
                            logger.debug("duration={}", duration);

                            Pattern datePattern = Pattern.compile("(.*)-(.*)-(.*)T(.*):(.*)");
                            Matcher m = datePattern.matcher(date);
                            if (m.matches()) {
                                String year = m.group(1);
                                String month = String.valueOf(Integer.parseInt(m.group(2)) - 1);
                                String day = m.group(3);

                                String hour = m.group(4);
                                String minute = m.group(5);
                                text = "<year>" + year + "</year>" +
                                    "<month>" + month + "</month>" +
                                    "<day>" + day + "</day>" +
                                    "<hour>" + hour + "</hour>" +
                                    "<minute>" + minute + "</minute>" +
                                    "<duration>" + duration + "</duration>";

                            } else {
                                text = StandardResponse.SCHEDULE_ERROR;
                            }
                        }
                    } else if (actions.has("say") && !hint.equals(PIC_HINT) && !hint.equals(SHOPPING_HINT)) {
                        logger.debug("in Sraix.sraixPannous, found say action");
                        Object obj = actions.get("say");
                        //MagicBooleans.trace("in Sraix.sraixPannous, obj class: " + obj.getClass());
                        //MagicBooleans.trace("in Sraix.sraixPannous, obj instanceof JSONObject: " + (obj instanceof JSONObject));
                        if (obj instanceof JSONObject) {
                            JSONObject sObj = (JSONObject) obj;
                            text = sObj.getString("text");
                            if (sObj.has("moreText")) {
                                JSONArray arr = sObj.getJSONArray("moreText");
                                for (int i = 0; i < arr.length(); i++) {
                                    text += " " + arr.getString(i);
                                }
                            }
                        } else {
                            text = obj.toString();
                        }
                    }
                    if (actions.has("show") && !text.contains("Wolfram")
                        && actions.getJSONObject("show").has("images")) {
                        logger.debug("in Sraix.sraixPannous, found show action");
                        JSONArray arr = actions.getJSONObject("show").getJSONArray(
                            "images");
                        int i = (int) (arr.length() * Math.random());
                        //for (int j = 0; j < arr.length(); j++) System.out.println(arr.getString(j));
                        imgRef = arr.getString(i);
                        if (imgRef.startsWith("//")) { imgRef = "http:" + imgRef; }
                        imgRef = "<a href=\"" + imgRef + "\"><img src=\"" + imgRef + "\"/></a>";
                        //System.out.println("IMAGE REF="+imgRef);

                    }
                    if (hint.equals(SHOPPING_HINT) && actions.has("open") && actions.getJSONObject("open").has("url")) {
                        urlRef = "<oob><url>" + actions.getJSONObject("open").getString("url") + "</oob></url>";

                    }
                }
                if (hint.equals(EVENT_HINT) && !text.startsWith("<year>")) {
                    return SRAIX_FAILED;
                } else if (text.equals(SRAIX_FAILED)) {
                    return new AIMLProcessor(chatSession).respond(SRAIX_FAILED, "nothing", "nothing");
                } else {
                    text = text.replace("&#39;", "'");
                    text = text.replace("&apos;", "'");
                    text = text.replaceAll("\\[(.*)\\]", "");
                    String[] sentences = text.split("\\. ");
                    //System.out.println("Sraix: text has "+sentences.length+" sentences:");
                    String clippedPage = sentences[0];
                    for (int i = 1; i < sentences.length; i++) {
                        if (clippedPage.length() < 500) { clippedPage = clippedPage + ". " + sentences[i]; }
                        //System.out.println(i+". "+sentences[i]);
                    }

                    clippedPage = clippedPage + " " + imgRef + " " + urlRef;
                    clippedPage = clippedPage.trim();
                    log(input, clippedPage);
                    return clippedPage;
                }
            }
        } catch (Exception ex) {
            logger.error("Sraix '{}' failed", input, ex);
        }
        return SRAIX_FAILED;
    } // sraixPannous

    private static String pannousCleanInput(String input) {
        return (" " + input + " ")
            .replace(" point ", ".")
            .replace(" rparen ", ")")
            .replace(" lparen ", "(")
            .replace(" slash ", "/")
            .replace(" star ", "*")
            .replace(" dash ", "-")
            .trim();
    }

    private static void log(String pattern, String template) {
        logger.info("Logging {}", pattern);
        template = template.trim();
        if (MagicBooleans.cache_sraix) {
            try {
                if (!template.contains("<year>") && !template.contains("No facilities")) {
                    template = template.replace("\n", "\\#Newline");
                    template = template.replace(",", MagicStrings.aimlif_split_char_name);
                    template = template.replaceAll("<a(.*)</a>", "");
                    template = template.trim();
                    if (!template.isEmpty()) {
                        Files.write(IOUtils.rootPath.resolve("bots/sraixcache/aimlif/sraixcache.aiml.csv"),
                            Arrays.asList("0," + pattern + ",*,*," + template + ",sraixcache.aiml"),
                            StandardOpenOption.APPEND);
                    }
                }
            } catch (Exception e) {
                logger.error("log error ", e);
            }
        }
    }
}
