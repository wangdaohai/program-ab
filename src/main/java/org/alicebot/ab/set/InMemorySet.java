package org.alicebot.ab.set;

import org.alicebot.ab.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/** Mutable Set */
public class InMemorySet extends AIMLSet {

    private static final Logger logger = LoggerFactory.getLogger(InMemorySet.class);
    private final Set<String> values = new HashSet<>();

    public InMemorySet(String name) {
        super(name);
    }

    @Override
    public boolean contains(String s) {
        return values.contains(s);
    }

    @Override
    public Set<String> values() {
        return Collections.unmodifiableSet(values);
    }

    public void add(String s) {
        values.add(s);
    }

    public void addAll(AIMLSet set) {
        values.addAll(set.values());
    }

    public void removeAll(AIMLSet set) {
        values.removeAll(set.values());
    }

    public void writeSet(Bot bot) {
        logger.info("Writing AIML Set {}", name());
        try {
            Stream<String> lines = values().stream().map(String::trim);
            Path setFile = bot.setsPath.resolve(name() + ".txt");
            Files.write(setFile, (Iterable<String>) lines::iterator);
        } catch (Exception e) {
            logger.error("writeSet error", e);
        }
    }

}
