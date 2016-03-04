package org.alicebot.ab.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;

public class IOUtils {

    private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);

    BufferedReader reader;
    BufferedWriter writer;

    public IOUtils(String filePath, String mode) {
        try {
            if ("read".equals(mode)) {
                reader = new BufferedReader(new FileReader(filePath));
            } else if ("write".equals(mode)) {
                (new File(filePath)).delete();
                writer = new BufferedWriter(new FileWriter(filePath, true));
            }
        } catch (IOException e) {
            logger.error("IOUtils error", e);
        }
    }

    public String readLine() {
        String result = null;
        try {
            result = reader.readLine();
        } catch (IOException e) {
            logger.error("readLine error ", e);
        }
        return result;
    }

    public void writeLine(String line) {
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            logger.error("writeLine error ", e);
        }
    }

    public void close() {
        try {
            if (reader != null) { reader.close(); }
            if (writer != null) { writer.close(); }
        } catch (IOException e) {
            logger.error("close error ", e);
        }

    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void writeOutputTextLine(String prompt, String text) {
        System.out.println(prompt + ": " + text);
    }

    public static String readInputTextLine() {
        return readInputTextLine(null);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static String readInputTextLine(String prompt) {
        if (prompt != null) {
            System.out.print(prompt + ": ");
        }
        BufferedReader lineOfText = new BufferedReader(new InputStreamReader(System.in));
        String textLine = null;
        try {
            textLine = lineOfText.readLine();
        } catch (IOException e) {
            logger.error("readInputTextLine error ", e);
        }
        return textLine;
    }

    public static File[] listFiles(File dir) {
        return dir.listFiles();
    }

    public static String system(String evaluatedContents, String failedString) {
        Runtime rt = Runtime.getRuntime();
        //System.out.println("System "+evaluatedContents);
        try {
            Process p = rt.exec(evaluatedContents);
            InputStream istrm = p.getInputStream();
            InputStreamReader istrmrdr = new InputStreamReader(istrm);
            BufferedReader buffrdr = new BufferedReader(istrmrdr);
            StringBuilder result = new StringBuilder();
            String data;
            while ((data = buffrdr.readLine()) != null) {
                result.append(data).append("\n");
            }
            //System.out.println("Result = "+result);
            return result.toString();
        } catch (Exception ex) {
            logger.error("system error ", ex);
            return failedString;

        }
    }

    public static String evalScript(String engineName, String script) throws Exception {
        //System.out.println("evaluating "+script);
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        return String.valueOf(engine.eval(script));
    }

}

