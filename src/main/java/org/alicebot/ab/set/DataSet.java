package org.alicebot.ab.set;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Immutable data-based AIML Set */
public class DataSet extends AIMLSet {

    private final Set<String> values;
    private int maxLength = -1;

    public DataSet(String name, Collection<String> values) {
        super(name);
        this.values = new HashSet<>(values);
    }

    @Override
    public boolean contains(String s) {
        return values.contains(s);
    }

    @Override
    public Set<String> values() {
        return Collections.unmodifiableSet(values);
    }

    @Override
    public int maxLength() {
        if (maxLength == -1) {
            maxLength = super.maxLength();
        }
        return maxLength;
    }
}
