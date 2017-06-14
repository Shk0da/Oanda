package org.pminin.tb.util;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeUtil {

    public static String rfc3339Plus2Days() {
        DateTime dateTime = new DateTime(System.currentTimeMillis() + (2 * 84600000), DateTimeZone.UTC);
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();

        return dateFormatter.print(dateTime);
    }

    public static String rfc3339FromLong(long millis) {
        DateTime dateTime = new DateTime(millis, DateTimeZone.UTC);
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();

        return dateFormatter.print(dateTime);
    }

}
