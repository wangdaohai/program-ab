package org.alicebot.ab.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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

    public static String date(String jformat, String locale, String timezone) {
        //HashSet<String> attributeNames = Utilities.stringSet("jformat","format","locale","timezone");
        if (jformat == null) { jformat = "EEE MMM dd HH:mm:ss zzz yyyy"; }
        if (locale == null) { locale = Locale.US.getISO3Country(); }
        if (timezone == null) { timezone = TimeZone.getDefault().getDisplayName(); }
        //System.out.println("Format = "+format+" Locale = "+locale+" Timezone = "+timezone);
        String dateAsString = new Date().toString();
        try {
            SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(jformat);
            dateAsString = simpleDateFormat.format(new Date());
        } catch (Exception ex) {
            logger.error("CalendarUtils.date Bad date: Format = {} Locale = {} Timezone = {}", jformat, locale, timezone, ex);
        }
        //MagicBooleans.trace("CalendarUtils.date: "+dateAsString);
        return dateAsString;
    }

}
