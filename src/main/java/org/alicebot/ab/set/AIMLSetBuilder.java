package org.alicebot.ab.set;

import org.alicebot.ab.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AIMLSetBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AIMLSetBuilder.class);
    private static final String REMOTE_SET_KEY = "external";

    private AIMLSetBuilder() {}

    public static Stream<AIMLSet> fromFolder(Path folder) throws IOException {
        if (!folder.toFile().exists()) {
            logger.warn("{} does not exist.", folder);
            return Stream.empty();
        }
        logger.debug("Loading AIML Sets files from {}", folder);
        return IOUtils.filesWithExtension(folder, ".txt")
            .map(path -> {
                try { return forPath(path); } catch (IOException e) {
                    logger.error("Failed to load set for path {}", path, e);
                    return null;
                }
            }).filter(s -> s != null);
    }

    private static AIMLSet forPath(Path path) throws IOException {
        String setName = IOUtils.basename(path);

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String firstLine = reader.readLine();
            if (firstLine.startsWith(REMOTE_SET_KEY)) {
                return buildRemoteSet(setName, firstLine);
            }
        }
        return new DataSet(setName, extractValues(path));
    }

    private static AIMLSet buildRemoteSet(String setName, String definitionLine) {
        String[] splitLine = definitionLine.split(":");
        if (splitLine.length < 4) {
            throw new IllegalArgumentException("External set without the correct number of arguments");
        }
        String host = splitLine[1];
        String botId = splitLine[2];
        int maxLength = Integer.parseInt(splitLine[3]);
        return new ExternalSet(setName, host, botId, maxLength);
    }

    private static Set<String> extractValues(Path path) throws IOException {
        return Files.lines(path)
            .map(l -> l.toUpperCase().trim())
            .collect(Collectors.toSet());
    }

}
