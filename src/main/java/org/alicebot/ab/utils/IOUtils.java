package org.alicebot.ab.utils;

import org.alicebot.ab.MagicStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class IOUtils {

    private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);

    private IOUtils() {}

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

    public static String evalScript(String script) throws ScriptException {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        return String.valueOf(engine.eval(script));
    }

    public static Stream<String> lines(Path path) {
        try {
            if (path.toFile().exists()) {
                return Files.lines(path)
                    .filter(line -> !line.startsWith(MagicStrings.text_comment_mark));
            }
            logger.error("{} does not exist", path);
        } catch (Exception e) {
            logger.error("lines error", e);
        }
        return Stream.empty();
    }

    public static String getFile(Path path) {
        return lines(path).collect(Collectors.joining("\n"));
    }
}

