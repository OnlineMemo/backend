package com.shj.onlinememospringproject.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeConverter {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  // "yyyy-MM-dd HH:mm:ss.SSS"
    public static final DateTimeFormatter RESPONSE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d. a h:mm").withLocale(Locale.forLanguageTag("ko"));
    public static final ZoneId KST_ZONEID = ZoneId.of("Asia/Seoul");


    public static String timeToStringForResponse(LocalDateTime localDateTime) {
        String strTime = localDateTime.format(RESPONSE_DATETIME_FORMATTER);
        return strTime;
    }

    public static LocalDateTime stringToTimeForResponse(String strTime) {  // 현재 미사용 메소드이나, 차후 활용가능성을 위해 작성해두었음.
        LocalDateTime localDateTime = LocalDateTime.parse(strTime, RESPONSE_DATETIME_FORMATTER);
        return localDateTime;
    }

    public static String longToStringForLog(Long unixTimestamp) {
        Instant instant = Instant.ofEpochSecond(unixTimestamp);
        ZonedDateTime kstDateTime = instant.atZone(KST_ZONEID);
        String strTime = kstDateTime.format(DATETIME_FORMATTER);
        return strTime;
    }
}
