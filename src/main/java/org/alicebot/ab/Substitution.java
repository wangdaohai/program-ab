package org.alicebot.ab;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Substitution {
    private final Pattern pattern;
    private final String substitution;

    public Substitution(Pattern pattern, String substitution) {
        this.pattern = pattern;
        this.substitution = substitution;
    }

    public String substitute(String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.replaceAll(substitution);
        }
        return input;
    }
}
