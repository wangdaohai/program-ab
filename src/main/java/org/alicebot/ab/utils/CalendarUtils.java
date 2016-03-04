package org.alicebot.ab.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class CalendarUtils {

    private static final Logger logger = LoggerFactory.getLogger(CalendarUtils.class);

    private CalendarUtils() {}

    public static String formatTime(String formatString, long msSinceEpoch) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(formatString);
        Calendar cal = Calendar.getInstance();
        dateFormat.setCalendar(cal);
        return dateFormat.format(new Date(msSinceEpoch));
    }

    public static int timeZoneOffset() {
        Calendar cal = Calendar.getInstance();
        return (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
    }

    public static String year() {
        Calendar cal = Calendar.getInstance();
        return String.valueOf(cal.get(Calendar.YEAR));
    }

    public static String date() {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMMMMMMM dd, yyyy");
        dateFormat.setCalendar(cal);
        return dateFormat.format(cal.getTime());
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
