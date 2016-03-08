package org.alicebot.ab;

import org.alicebot.ab.aiml.AIMLDefault;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HistoryTest {

    @Test
    public void enqueueOrder() {
        History<String> history = History.ofString("plop");
        history.add("first");
        history.add("second");
        history.add("third");
        assertEquals("third", history.get(0));
        assertEquals("second", history.get(1));
        assertEquals("first", history.get(2));
        history.printHistory();
    }

    @Test
    public void unknownItem() {
        History<String> history = History.ofString("plop");
        assertEquals(AIMLDefault.unknown_history_item, history.get(12));
    }

    @Test
    public void forgetHistory() {
        History<String> history = History.ofString("plop");
        IntStream.range(0, MagicNumbers.max_history * 2).mapToObj(String::valueOf).forEach(history::add);
        assertNull(history.get(MagicNumbers.max_history));
        assertEquals(String.valueOf(MagicNumbers.max_history), history.get(MagicNumbers.max_history - 1));
        assertEquals(String.valueOf(MagicNumbers.max_history * 2 - 1), history.get(0));
    }

}
