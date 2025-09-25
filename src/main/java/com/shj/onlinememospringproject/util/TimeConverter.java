package com.shj.onlinememospringproject.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeConverter {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d. a h:mm").withLocale(Locale.forLanguageTag("ko"));
    public static final DateTimeFormatter LOG_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  // "yyyy-MM-dd HH:mm:ss.SSS"
    public static final ZoneId KST_ZONEID = ZoneId.of("Asia/Seoul");


    public static String timeToString(LocalDateTime localDateTime) {
        String strTime = localDateTime.format(FORMATTER);
        return strTime;
    }

    public static LocalDateTime stringToTime(String strTime) {  // 현재 미사용 메소드이나, 차후 활용가능성을 위해 작성해두었음.
        LocalDateTime localDateTime = LocalDateTime.parse(strTime, FORMATTER);
        return localDateTime;
    }

    public static String longToStringForLog(Long unixTimestamp) {
        Instant instant = Instant.ofEpochSecond(unixTimestamp);
        ZonedDateTime kstDateTime = instant.atZone(KST_ZONEID);
        String strTime = kstDateTime.format(LOG_FORMATTER);
        return strTime;
    }
}
