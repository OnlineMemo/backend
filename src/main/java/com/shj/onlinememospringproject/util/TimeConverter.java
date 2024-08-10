package com.shj.onlinememospringproject.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeConverter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d. a h:mm").withLocale(Locale.forLanguageTag("ko"));


    public static String timeToString(LocalDateTime localDateTime) {
        String strTime = localDateTime.format(FORMATTER);
        return strTime;
    }

    public static LocalDateTime stringToTime(String strTime) {
        LocalDateTime localDateTime = LocalDateTime.parse(strTime, FORMATTER);
        return localDateTime;
    }
}
