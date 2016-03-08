package org.alicebot.ab.set;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** AIML Set whose content is computed on-the-fly */
public abstract class ComputeSet extends AIMLSet {

    private static final String NATURAL_NUMBER_SET_NAME = "number";

    public ComputeSet(String name) {
        super(name);
    }

    @Override
    public Set<String> values() {
        return Collections.emptySet();
    }

    public static final ComputeSet NATURAL_NUMBERS = new ComputeSet(NATURAL_NUMBER_SET_NAME) {

        private final Pattern numberPattern = Pattern.compile("[0-9]+");

        @Override
        public boolean contains(String s) {
            Matcher numberMatcher = numberPattern.matcher(s);
            return numberMatcher.matches();
        }
    };

}
