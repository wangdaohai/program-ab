package org.alicebot.ab.map;
/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Transforms words to singular, plural, humanized (human readable) or ordinal form. This is inspired by
 * the <a href="http://api.rubyonrails.org/classes/Inflector.html">Inflector</a> class in <a
 * href="http://www.rubyonrails.org">Ruby on Rails</a>, which is distributed under the <a
 * href="http://wiki.rubyonrails.org/rails/pages/License">Rails license</a>.
 *
 * @author Randall Hauch
 */
public enum Inflector {
    INSTANCE;

    protected static class Rule {

        protected final String expression;
        protected final Pattern expressionPattern;
        protected final String replacement;

        protected Rule(String expression,
                       String replacement) {
            this.expression = expression;
            this.replacement = replacement != null ? replacement : "";
            this.expressionPattern = Pattern.compile(this.expression, Pattern.CASE_INSENSITIVE);
        }

        /**
         * Apply the rule against the input string, returning the modified string or null if the rule didn't apply (and no
         * modifications were made)
         *
         * @param input the input string
         * @return the modified string if this rule applied, or null if the input was not modified by this rule
         */
        protected String apply(String input) {
            Matcher matcher = this.expressionPattern.matcher(input);
            if (!matcher.find()) { return null; }
            return matcher.replaceAll(this.replacement);
        }

        @Override
        public int hashCode() {
            return expression.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) { return true; }
            if (obj != null && obj.getClass() == this.getClass()) {
                final Rule that = (Rule) obj;
                if (this.expression.equalsIgnoreCase(that.expression)) { return true; }
            }
            return false;
        }

        @Override
        public String toString() {
            return expression + ", " + replacement;
        }
    }

    private final LinkedList<Rule> plurals = new LinkedList<>();
    private final LinkedList<Rule> singulars = new LinkedList<>();
    /**
     * The lowercase words that are to be excluded and not processed. This map can be modified by the users via
     */
    private final Set<String> uncountables;

    // ------------------------------------------------------------------------------------------------
    // Usage functions
    // ------------------------------------------------------------------------------------------------

    /**
     * Returns the plural form of the word in the string.
     * <p>
     * Examples:
     * <pre>
     *   inflector.pluralize(&quot;post&quot;)               #=&gt; &quot;posts&quot;
     *   inflector.pluralize(&quot;octopus&quot;)            #=&gt; &quot;octopi&quot;
     *   inflector.pluralize(&quot;sheep&quot;)              #=&gt; &quot;sheep&quot;
     *   inflector.pluralize(&quot;words&quot;)              #=&gt; &quot;words&quot;
     *   inflector.pluralize(&quot;the blue mailman&quot;)   #=&gt; &quot;the blue mailmen&quot;
     *   inflector.pluralize(&quot;CamelOctopus&quot;)       #=&gt; &quot;CamelOctopi&quot;
     * </pre>
     * <p>
     * Note that if the {@link Object#toString()} is called on the supplied object, so this method works for non-strings, too.
     *
     * @param word the word that is to be pluralized.
     * @return the pluralized form of the word, or the word itself if it could not be pluralized
     * @see #singularize(Object)
     */
    public String pluralize(Object word) {
        if (word == null) { return null; }
        String wordStr = word.toString().trim();
        if (wordStr.isEmpty()) { return wordStr; }
        if (isUncountable(wordStr)) { return wordStr; }
        for (Rule rule : this.plurals) {
            String result = rule.apply(wordStr);
            if (result != null) { return result; }
        }
        return wordStr;
    }

    /**
     * Returns the singular form of the word in the string.
     * <p>
     * Examples:
     * <pre>
     *   inflector.singularize(&quot;posts&quot;)             #=&gt; &quot;post&quot;
     *   inflector.singularize(&quot;octopi&quot;)            #=&gt; &quot;octopus&quot;
     *   inflector.singularize(&quot;sheep&quot;)             #=&gt; &quot;sheep&quot;
     *   inflector.singularize(&quot;words&quot;)             #=&gt; &quot;word&quot;
     *   inflector.singularize(&quot;the blue mailmen&quot;)  #=&gt; &quot;the blue mailman&quot;
     *   inflector.singularize(&quot;CamelOctopi&quot;)       #=&gt; &quot;CamelOctopus&quot;
     * </pre>
     * <p>
     * Note that if the {@link Object#toString()} is called on the supplied object, so this method works for non-strings, too.
     *
     * @param word the word that is to be pluralized.
     * @return the pluralized form of the word, or the word itself if it could not be pluralized
     * @see #pluralize(Object)
     */
    public String singularize(Object word) {
        if (word == null) { return null; }
        String wordStr = word.toString().trim();
        if (wordStr.isEmpty()) { return wordStr; }
        if (isUncountable(wordStr)) { return wordStr; }
        for (Rule rule : this.singulars) {
            String result = rule.apply(wordStr);
            if (result != null) { return result; }
        }
        return wordStr;
    }

    // ------------------------------------------------------------------------------------------------
    // Management methods
    // ------------------------------------------------------------------------------------------------

    /**
     * Determine whether the supplied word is considered uncountable by the {@link #pluralize(Object) pluralize} and
     * {@link #singularize(Object) singularize} methods.
     *
     * @return true if the plural and singular forms of the word are the same
     */
    private boolean isUncountable(String word) {
        if (word == null) { return false; }
        String trimmedLower = word.trim().toLowerCase();
        return this.uncountables.contains(trimmedLower);
    }

    private void addPluralize(String rule, String replacement) {
        plurals.addFirst(new Rule(rule, replacement));
    }

    private void addSingularize(String rule, String replacement) {
        singulars.addFirst(new Rule(rule, replacement));
    }

    private void addIrregular(String singular, String plural) {
        //CheckArg.isNotEmpty(singular, "singular rule");
        //CheckArg.isNotEmpty(plural, "plural rule");
        String singularRemainder = singular.length() > 1 ? singular.substring(1) : "";
        String pluralRemainder = plural.length() > 1 ? plural.substring(1) : "";
        addPluralize("(" + singular.charAt(0) + ")" + singularRemainder + "$", "$1" + pluralRemainder);
        addSingularize("(" + plural.charAt(0) + ")" + pluralRemainder + "$", "$1" + singularRemainder);
    }

    Inflector() {
        addPluralize("$", "s");
        addPluralize("s$", "s");
        addPluralize("(ax|test)is$", "$1es");
        addPluralize("(octop|vir)us$", "$1i");
        addPluralize("(octop|vir)i$", "$1i"); // already plural
        addPluralize("(alias|status)$", "$1es");
        addPluralize("(bu)s$", "$1ses");
        addPluralize("(buffal|tomat)o$", "$1oes");
        addPluralize("([ti])um$", "$1a");
        addPluralize("([ti])a$", "$1a"); // already plural
        addPluralize("sis$", "ses");
        addPluralize("(?:([^f])fe|([lr])f)$", "$1$2ves");
        addPluralize("(hive)$", "$1s");
        addPluralize("([^aeiouy]|qu)y$", "$1ies");
        addPluralize("(x|ch|ss|sh)$", "$1es");
        addPluralize("(matr|vert|ind)ix|ex$", "$1ices");
        addPluralize("([m|l])ouse$", "$1ice");
        addPluralize("([m|l])ice$", "$1ice");
        addPluralize("^(ox)$", "$1en");
        addPluralize("(quiz)$", "$1zes");
        // Need to check for the following words that are already pluralized:
        addPluralize("(people|men|children|sexes|moves|stadiums)$", "$1"); // irregulars
        addPluralize("(oxen|octopi|viri|aliases|quizzes)$", "$1"); // special rules

        addSingularize("s$", "");
        addSingularize("(s|si|u)s$", "$1s"); // '-us' and '-ss' are already singular
        addSingularize("(n)ews$", "$1ews");
        addSingularize("([ti])a$", "$1um");
        addSingularize("((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "$1$2sis");
        addSingularize("(^analy)ses$", "$1sis");
        addSingularize("(^analy)sis$", "$1sis"); // already singular, but ends in 's'
        addSingularize("([^f])ves$", "$1fe");
        addSingularize("(hive)s$", "$1");
        addSingularize("(tive)s$", "$1");
        addSingularize("([lr])ves$", "$1f");
        addSingularize("([^aeiouy]|qu)ies$", "$1y");
        addSingularize("(s)eries$", "$1eries");
        addSingularize("(m)ovies$", "$1ovie");
        addSingularize("(x|ch|ss|sh)es$", "$1");
        addSingularize("([m|l])ice$", "$1ouse");
        addSingularize("(bus)es$", "$1");
        addSingularize("(o)es$", "$1");
        addSingularize("(shoe)s$", "$1");
        addSingularize("(cris|ax|test)is$", "$1is"); // already singular, but ends in 's'
        addSingularize("(cris|ax|test)es$", "$1is");
        addSingularize("(octop|vir)i$", "$1us");
        addSingularize("(octop|vir)us$", "$1us"); // already singular, but ends in 's'
        addSingularize("(alias|status)es$", "$1");
        addSingularize("(alias|status)$", "$1"); // already singular, but ends in 's'
        addSingularize("^(ox)en", "$1");
        addSingularize("(vert|ind)ices$", "$1ex");
        addSingularize("(matr)ices$", "$1ix");
        addSingularize("(quiz)zes$", "$1");

        addIrregular("person", "people");
        addIrregular("man", "men");
        addIrregular("child", "children");
        addIrregular("sex", "sexes");
        addIrregular("move", "moves");
        addIrregular("stadium", "stadiums");

        uncountables = Stream.of("equipment", "information", "rice", "money", "species", "series", "fish", "sheep")
            .map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());
    }

}