package org.alicebot.ab.map;

import org.alicebot.ab.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AIMLMapBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AIMLMapBuilder.class);
    private static final String REMOTE_MAP_KEY = "external";

    private AIMLMapBuilder() {}

    public static Stream<AIMLMap> fromFolder(Path folder) throws IOException {
        if (!folder.toFile().exists()) {
            logger.warn("{} does not exist.", folder);
            return Stream.empty();
        }
        logger.debug("Loading AIML Map files from {}", folder);
        return IOUtils.filesWithExtension(folder, ".txt")
            .map(path -> {
                try { return forPath(path); } catch (IOException e) {
                    logger.error("Failed to load set for path {}", path, e);
                    return null;
                }
            }).filter(s -> s != null);
    }

    private static AIMLMap forPath(Path path) throws IOException {
        String name = IOUtils.basename(path);
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String firstLine = reader.readLine();
            if (firstLine.startsWith(REMOTE_MAP_KEY)) {
                return buildRemoteMap(name, firstLine);
            }
        }
        return new DataMap(name, extractValues(path));
    }

    private static AIMLMap buildRemoteMap(String name, String firstLine) {
        String[] splitLine = firstLine.split(":");
        if (splitLine.length < 3) {
            throw new IllegalArgumentException("External map without the correct number of arguments");
        }
        String host = splitLine[1];
        String botId = splitLine[2];
        logger.info("Creating external map at {} {}", host, botId);
        return new ExternalMap(name, host, botId);
    }

    private static Map<String, String> extractValues(Path path) throws IOException {
        return Files.lines(path).map(l -> l.split(":")).filter(l -> l.length >= 2)
            .collect(Collectors.toMap(a -> a[0].toUpperCase(), a -> a[1], (v1, v2) -> {
                logger.debug("Duplicate values in {}: {}, {}", path, v1, v2);
                return v2;
            }));
    }

}
