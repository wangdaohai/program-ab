package org.alicebot.ab.utils;

import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Months;
import org.joda.time.Years;
import org.joda.time.chrono.GregorianChronology;
import org.joda.time.chrono.LenientChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class IntervalUtils {

    private static final Logger logger = LoggerFactory.getLogger(IntervalUtils.class);

    private IntervalUtils() {}

    public static Optional<Integer> getInterval(String from, String to, String jformat, String style) {
        if ("years".equals(style)) { return Optional.of(getYearsBetween(from, to, jformat)); }
        if ("months".equals(style)) { return Optional.of(getMonthsBetween(from, to, jformat)); }
        if ("days".equals(style)) { return Optional.of(getDaysBetween(from, to, jformat)); }
        if ("hours".equals(style)) { return Optional.of(getHoursBetween(from, to, jformat)); }
        return Optional.empty();
    }

    // http://docs.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
    public static int getHoursBetween(final String date1, final String date2, String format) {
        try {
            final DateTimeFormatter fmt = getDateTimeFormatter(format);
            return Hours.hoursBetween(
                fmt.parseDateTime(date1),
                fmt.parseDateTime(date2)
            ).getHours();
        } catch (Exception ex) {
            logger.error("getHoursBetween error", ex);
            return 0;
        }
    }

    public static int getYearsBetween(final String date1, final String date2, String format) {
        try {
            final DateTimeFormatter fmt = getDateTimeFormatter(format);
            return Years.yearsBetween(
                fmt.parseDateTime(date1),
                fmt.parseDateTime(date2)
            ).getYears();
        } catch (Exception ex) {
            logger.error("getYearsBetween error", ex);
            return 0;
        }
    }

    public static int getMonthsBetween(final String date1, final String date2, String format) {
        try {
            final DateTimeFormatter fmt = getDateTimeFormatter(format);
            return Months.monthsBetween(
                fmt.parseDateTime(date1),
                fmt.parseDateTime(date2)
            ).getMonths();
        } catch (Exception ex) {
            logger.error("getMonthsBetween error", ex);
            return 0;
        }
    }

    public static int getDaysBetween(final String date1, final String date2, String format) {
        try {
            final DateTimeFormatter fmt = getDateTimeFormatter(format);
            return Days.daysBetween(
                fmt.parseDateTime(date1),
                fmt.parseDateTime(date2)
            ).getDays();
        } catch (Exception ex) {
            logger.error("getDaysBetween error", ex);
            return 0;
        }
    }

    private static DateTimeFormatter getDateTimeFormatter(String format) {
        return DateTimeFormat.forPattern(format)
            .withChronology(LenientChronology.getInstance(GregorianChronology.getInstance()));
    }
}
