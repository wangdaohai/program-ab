package org.alicebot.ab.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

public class IOUtils {

    private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);

    BufferedReader reader;
    BufferedWriter writer;

    public IOUtils(Path filePath, String mode) {
        try {
            if ("read".equals(mode)) {
                reader = Files.newBufferedReader(filePath);
            } else if ("write".equals(mode)) {
                writer = Files.newBufferedWriter(filePath, StandardOpenOption.TRUNCATE_EXISTING);
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

    public static Stream<Path> filesWithExtension(Path folder, String extension) throws IOException {
        String lowerCaseExt = extension.toLowerCase();
        return Files.list(folder)
            .filter(f -> f.toFile().isFile())
            .filter(f -> f.toString().toLowerCase().endsWith(lowerCaseExt));
    }

    public static String basename(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
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

