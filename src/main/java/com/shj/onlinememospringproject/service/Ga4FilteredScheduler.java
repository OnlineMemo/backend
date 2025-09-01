package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import com.shj.onlinememospringproject.repository.Ga4FilteredRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class Ga4FilteredScheduler {

    private final Ga4FilteredService ga4FilteredService;
    private final Ga4FilteredRepository ga4FilteredRepository;
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Scheduled(cron = "0 0 15 * * ?", zone = "Asia/Seoul")  // 매일 오후 3시에 실행
    public void filterAndSaveGa4() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        String startDayStr;
        String endDayStr = yesterday.format(FORMATTER);  // 하루 전의 데이터까지 정제함.

        Ga4Filtered recentGa4Filtered = ga4FilteredRepository.findFirstByOrderByEventDatetimeDesc().orElse(null);
        if(recentGa4Filtered == null) {
            startDayStr = "2025-08-01";
        }
        else {
            LocalDate recentDay = recentGa4Filtered.getEventDatetime().toLocalDate();
            startDayStr = recentDay.plusDays(1).format(FORMATTER);
        }

        if(startDayStr.compareTo(endDayStr) > 0) return;
        String startDatetimeStr = startDayStr + " 00:00:00";
        String endDatetimeStr = endDayStr + " 23:59:59";

        ga4FilteredService.filterAndSaveGa4(startDatetimeStr, endDatetimeStr);
    }
}
