package org.alicebot.ab.map;

import org.alicebot.ab.MagicStrings;

public abstract class ComputeMap extends AIMLMap {

    public ComputeMap(String name) {
        super(name);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String toString() {
        return ComputeMap.class.getSimpleName() + "{" + name() + "}";
    }

    public static final ComputeMap SUCCESSOR = new ComputeMap(MagicStrings.map_successor) {
        @Override
        public String get(String key) {
            try {
                int number = Integer.parseInt(key);
                return String.valueOf(number + 1);
            } catch (Exception ex) {
                return MagicStrings.default_map;
            }
        }
    };

    public static final ComputeMap PREDECESSOR = new ComputeMap(MagicStrings.map_predecessor) {
        @Override
        public String get(String key) {
            try {
                int number = Integer.parseInt(key);
                return String.valueOf(number - 1);
            } catch (Exception ex) {
                return MagicStrings.default_map;
            }
        }
    };

    public static final ComputeMap SINGULAR = new ComputeMap(MagicStrings.map_singular) {
        @Override
        public String get(String key) {
            return Inflector.INSTANCE.singularize(key);
        }
    };

    public static final ComputeMap PLURAL = new ComputeMap(MagicStrings.map_plural) {
        @Override
        public String get(String key) {
            return Inflector.INSTANCE.pluralize(key);
        }
    };
}
