package org.alicebot.ab.map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
// http://stackoverflow.com/questions/4757800/configuring-intellij-idea-for-unit-testing-with-junit

/**
 * @since 3/31/14.
 */
public class InflectorTest {
    @Test
    public void testPluralize() throws Exception {
        String[][] pairs = {{"dog", "dogs"}, {"person", "people"}, {"cats", "cats"}};
        for (String[] pair : pairs) {
            String singular = pair[0];
            String plural = pair[1];
            assertEquals(plural, Inflector.INSTANCE.pluralize(singular));
        }

    }

    @Test
    public void testSingularize() throws Exception {
        String[][] pairs = {{"dog", "dogs"}, {"person", "people"}, {"cat", "cat"}};
        for (String[] pair : pairs) {
            String singular = pair[0];
            String plural = pair[1];
            assertEquals(singular, Inflector.INSTANCE.singularize(plural));
        }
    }
}
