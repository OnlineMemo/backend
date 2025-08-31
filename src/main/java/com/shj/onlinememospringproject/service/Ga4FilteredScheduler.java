package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.backoffice.State;
import com.shj.onlinememospringproject.repository.StateRepository;
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
    private final StateRepository stateRepository;
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Scheduled(cron = "0 0 15 * * ?", zone = "Asia/Seoul")  // 매일 오후 3시에 실행
    public void filterAndSaveGa4() {
        State state = stateRepository.findFirstByOrderByRecentDatetimeDesc()
                .orElseGet(() -> {
                    State newState = State.StateSaveBuilder()
                            .recentDatetime(null)
                            .build();
                    return stateRepository.save(newState);
                });

        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        String startDayStr;
        String endDayStr = yesterday.format(FORMATTER);  // 하루 전의 데이터까지 정제함.

        if(state.getRecentDatetime() == null) {
            startDayStr = "2025-08-01";
        }
        else {
            LocalDate recentDay = state.getRecentDatetime().toLocalDate();
            startDayStr = recentDay.plusDays(1).format(FORMATTER);
        }

        if(startDayStr.compareTo(endDayStr) > 0) return;
        String startDatetimeStr = startDayStr + " 00:00:00";
        String endDatetimeStr = endDayStr + " 23:59:59";

        ga4FilteredService.filterAndSaveGa4(startDatetimeStr, endDatetimeStr);
        stateRepository.updateRecentDatetimeById(state.getId(), yesterday.atTime(23, 59, 59));  // 23:59:59
    }
}
