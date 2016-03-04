package org.alicebot.ab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
// http://stackoverflow.com/questions/4757800/configuring-intellij-idea-for-unit-testing-with-junit

/**
 * Created by User on 3/31/14.
 */
public class InflectorTest {
    @Test
    public void testPluralize() throws Exception {
        Inflector inflector = new Inflector();
        String[][] pairs = {{"dog", "dogs"}, {"person", "people"}, {"cats", "cats"}};
        for (String[] pair : pairs) {
            String singular = pair[0];
            String expected = pair[1];
            String actual = inflector.pluralize(singular);
            assertEquals("Pluralize " + pairs[0][0], expected, actual);
        }

    }

    @Test
    public void testSingularize() throws Exception {

    }
}
