package org.alicebot.ab.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class CalendarUtils {

    private static final Logger logger = LoggerFactory.getLogger(CalendarUtils.class);

    private CalendarUtils() {}

    public static int timeZoneOffset() {
        return LocalDateTime.now().atZone(ZoneId.systemDefault())
            .getOffset().getTotalSeconds() / 60;
    }

    public static String year() {
        return String.valueOf(LocalDate.now().getYear());
    }

    public static String date() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }

    public static String date(String jformat) {
        return format(new Date(), jformat);
    }

    public static String format(Date date, String jformat) {
        if (jformat == null) { jformat = "EEE MMM dd HH:mm:ss zzz yyyy"; }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(jformat);
            return simpleDateFormat.format(date);
        } catch (Exception ex) {
            logger.error("CalendarUtils.format Bad date format = {}", jformat, ex);
            return date.toString();
        }
    }

}
