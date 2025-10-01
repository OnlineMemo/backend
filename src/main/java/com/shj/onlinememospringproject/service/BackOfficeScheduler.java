package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import com.shj.onlinememospringproject.repository.Ga4FilteredRepository;
import com.shj.onlinememospringproject.repository.RedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.shj.onlinememospringproject.service.impl.Ga4FilteredServiceImpl.GA4FILTERED_FORMATTER;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackOfficeScheduler {

    public static final double MB_DIVISOR = 1024.0 * 1024.0;
    private static final Marker WARN_OOM_LOG_MARKER = MarkerFactory.getMarker("WARN_OOM_LOG");
    private static final String WARN_OOM_LOG_KEY = "backoffice:heap_memory_notification";

    private final Ga4FilteredService ga4FilteredService;
    private final Ga4FilteredRepository ga4FilteredRepository;
    private final RedisRepository redisRepository;


    @Scheduled(cron = "0 0 15 * * ?", zone = "Asia/Seoul")  // 매일 오후 3시에 실행
    public void filterAndSaveGa4() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        String startDayStr;
        String endDayStr = yesterday.format(GA4FILTERED_FORMATTER);  // 하루 전의 데이터까지 정제함.

        Ga4Filtered recentGa4Filtered = ga4FilteredRepository.findFirstByOrderByEventDatetimeDesc().orElse(null);
        if(recentGa4Filtered == null) {
            startDayStr = "2025-08-01";
        }
        else {
            LocalDate recentDay = recentGa4Filtered.getEventDatetime().toLocalDate();
            startDayStr = recentDay.plusDays(1).format(GA4FILTERED_FORMATTER);
        }

        if(startDayStr.compareTo(endDayStr) > 0) return;
        String startDatetimeStr = startDayStr + " 00:00:00";
        String endDatetimeStr = endDayStr + " 23:59:59";

        ga4FilteredService.filterAndSaveGa4(startDatetimeStr, endDatetimeStr);
    }

    @Scheduled(fixedRate = 1000 * 60 * 5)  // 5분 간격으로 실행
    public void checkHeapMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();

        double limitMaxMB = runtime.maxMemory() / MB_DIVISOR;  // 설정된 JVM 한도의 최대 힙메모리 (한계치)
        double currentMaxMB = runtime.totalMemory() / MB_DIVISOR;  // 현재 OS에서 할당받은 최대 힙메모리 (점점 늘어남)
        double usedMB = currentMaxMB - (runtime.freeMemory() / MB_DIVISOR);
        double usedPercent = usedMB * 100 / limitMaxMB;
        usedPercent = Math.round(usedPercent * 100) / 100.0;

        if(usedPercent >= 70) {
            String value = redisRepository.getValue(WARN_OOM_LOG_KEY);
            if(value != null) return;

            double remainMB = limitMaxMB - usedMB;
            int warnPercent = (usedPercent >= 90) ? 90
                    : (usedPercent >= 80) ? 80 : 70;

            log.warn(WARN_OOM_LOG_MARKER,
                    String.format("힙메모리 %d%% 위험\n-  최대: 100%% (%.2fMB · %.2fGB)\n-  사용: %.2f%% (%.2fMB · %.2fGB)\n-  잔여: %.2f%% (%.2fMB · %.2fGB)",  // Slack Template
                            warnPercent, limitMaxMB, limitMaxMB/1024, usedPercent, usedMB, usedMB/1024, remainMB*100/limitMaxMB, remainMB, remainMB/1024));

            Long ttlMillisecond = Duration.ofHours(1).toMillis();
            redisRepository.setValue(WARN_OOM_LOG_KEY, "true", ttlMillisecond);  // 실상 value 값은 무의미한 형식적인 것임.
        }
    }
}
