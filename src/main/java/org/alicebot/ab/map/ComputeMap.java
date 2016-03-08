package org.alicebot.ab.map;

import org.alicebot.ab.aiml.AIMLDefault;

import java.util.function.UnaryOperator;

public class ComputeMap extends AIMLMap {

    public static final String MAP_SUCCESSOR = "successor";
    public static final String MAP_PREDECESSOR = "predecessor";
    public static final String MAP_SINGULAR = "singular";
    public static final String MAP_PLURAL = "plural";

    private final UnaryOperator<String> computation;

    public ComputeMap(String name, UnaryOperator<String> computation) {
        super(name);
        this.computation = computation;
    }

    @Override
    public String get(String key) {
        return computation.apply(key);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return ComputeMap.class.getSimpleName() + "{" + name() + "}";
    }

    public static final ComputeMap SUCCESSOR = new ComputeMap(MAP_SUCCESSOR, key -> {
        try {
            int number = Integer.parseInt(key);
            return String.valueOf(number + 1);
        } catch (Exception ex) {
            return AIMLDefault.default_map;
        }
    });

    public static final ComputeMap PREDECESSOR = new ComputeMap(MAP_PREDECESSOR, key -> {
        try {
            int number = Integer.parseInt(key);
            return String.valueOf(number - 1);
        } catch (Exception ex) {
            return AIMLDefault.default_map;
        }
    });

    public static final ComputeMap SINGULAR = new ComputeMap(MAP_SINGULAR, Inflector.INSTANCE::singularize);

    public static final ComputeMap PLURAL = new ComputeMap(MAP_PLURAL, Inflector.INSTANCE::pluralize);
}
