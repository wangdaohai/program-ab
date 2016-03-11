package org.alicebot.ab;

import org.junit.Test;

import static org.junit.Assert.*;

public class PathTest {

    @Test
    public void emptySentence() {
        Path emptyPath = Path.fromSentence("");
        assertPath(emptyPath, 1, "", true);
    }

    @Test
    public void oneWord() {
        Path oneWord = Path.fromSentence("hello");
        assertPath(oneWord, 1, "hello", true);
    }

    @Test
    public void severalWords() {
        Path path = Path.fromSentence("this is nice");
        assertPath(path, 3, "this", false);
        assertPath(path.next, 2, "is", false);
        assertPath(path.next.next, 1, "nice", true);

    }

    private void assertPath(Path path, int length, String word, boolean nullNext) {
        assertEquals(length, path.length);
        assertEquals(word, path.word);
        if (nullNext) {
            assertNull(path.next);
        } else {
            assertNotNull(path.next);
        }
    }

    @Test
    public void toSentence() {
        assertEquals("this is nice", Path.toSentence(Path.fromSentence("this is nice")));
    }

    @Test
    public void toStringTest() {
        assertEquals("this,is,nice", Path.fromSentence("this is nice").toString());
    }

}
