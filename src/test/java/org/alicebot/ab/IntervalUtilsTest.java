package org.alicebot.ab;

import org.alicebot.ab.utils.IntervalUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IntervalUtilsTest {

    @Test
    public void test() {
        int hours = IntervalUtils.getHoursBetween(
            "12:00:00.00",
            "23:59:59.00",
            "HH:mm:ss.SS"
        );
        assertEquals(11, hours);
        int years = IntervalUtils.getYearsBetween(
            "August 2, 1960",
            "January 30, 2013",
            "MMMMMMMMM dd, yyyy"
        );
        assertEquals(52, years);
    }
}
