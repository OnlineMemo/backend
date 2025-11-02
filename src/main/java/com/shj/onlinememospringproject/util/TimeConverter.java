package com.shj.onlinememospringproject.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeConverter {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  // or "yyyy-MM-dd HH:mm:ss.SSS"
    public static final DateTimeFormatter RESPONSE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d. a h:mm").withLocale(Locale.forLanguageTag("ko"));
    public static final String DURATION_STRING_FORMATTER = "%d일 %02d:%02d:%02d";  // ex) "3일 02:40:12"
    public static final ZoneId KST_ZONEID = ZoneId.of("Asia/Seoul");


    public static String timeToStringForResponse(LocalDateTime localDateTime) {
        String strTime = localDateTime.format(RESPONSE_DATETIME_FORMATTER);
        return strTime;
    }

    public static LocalDateTime stringToTimeForResponse(String strTime) {  // 현재 미사용 메소드이나, 차후 활용가능성을 위해 작성해두었음.
        LocalDateTime localDateTime = LocalDateTime.parse(strTime, RESPONSE_DATETIME_FORMATTER);
        return localDateTime;
    }

    public static String longToStringForLog(long unixTimestamp) {
        Instant instant = Instant.ofEpochSecond(unixTimestamp);
        ZonedDateTime kstDateTime = instant.atZone(KST_ZONEID);
        String strTime = kstDateTime.format(DATETIME_FORMATTER);
        return strTime;
    }

    public static String secondsToStringForDuration(long totalSeconds) {
        if(totalSeconds <= 0) return "0초";
        long days = totalSeconds / 86400;  // 60초 x 60분 x 24시간 = 86400 (1일)
        long hours = (totalSeconds % 86400) / 3600;  // 60초 x 60분 = 3600 (1시간)
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if(days > 0) return String.format(DURATION_STRING_FORMATTER, days, hours, minutes, seconds);
        StringBuilder durationStb = new StringBuilder();
        if(hours > 0) durationStb.append(hours).append("시간 ");
        if(minutes > 0) durationStb.append(minutes).append("분 ");
        if(seconds > 0) durationStb.append(seconds).append("초");

        return durationStb.toString().strip();
    }
}
