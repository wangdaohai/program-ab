package org.alicebot.ab.set;

import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.utils.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public final class AIMLSetBuilder {

    private AIMLSetBuilder() {}

    public static AIMLSet forPath(Path path) throws IOException {
        String setName = IOUtils.basename(path);

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String firstLine = reader.readLine();
            if (firstLine.startsWith(MagicStrings.remote_set_key)) {
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
