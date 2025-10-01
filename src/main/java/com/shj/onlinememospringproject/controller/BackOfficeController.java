package com.shj.onlinememospringproject.controller;

import com.shj.onlinememospringproject.dto.Ga4FilteredDto;
import com.shj.onlinememospringproject.dto.UserDto;
import com.shj.onlinememospringproject.response.ResponseCode;
import com.shj.onlinememospringproject.response.ResponseData;
import com.shj.onlinememospringproject.service.Ga4FilteredService;
import com.shj.onlinememospringproject.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.shj.onlinememospringproject.service.BackOfficeScheduler.MB_DIVISOR;

@Tag(name = "BackOffice")
@RestController
@RequiredArgsConstructor
@RequestMapping("/back-office")
public class BackOfficeController {

    private final Ga4FilteredService ga4FilteredService;
    private final UserService userService;


    @GetMapping("/ga4/all-data")
    @Operation(summary = "GA4 전체 데이터 조회 [JWT O]",
            description = """
                    <strong>< RequestParam ></strong>
                    - <strong>startDatetime, endDatetime</strong> : "2025-08-29 23:59:59" 형태의 날짜시각  \n
                    <strong>< URI ></strong>
                    - <strong>예시 URI</strong> : /back-office/ga4/all-data?startDatetime=2025-08-01%2015:30:00&endDatetime=2025-08-29%2023:59:59
                    - <strong>참고 사항</strong> : 중간의 '%20'은 공백을 의미
                    """)
    public ResponseEntity<ResponseData<List<Ga4FilteredDto.Response>>> findGa4FilteredAll(
            @RequestParam(value = "startDatetime", required = true) String startDatetime,
            @RequestParam(value = "endDatetime", required = true) String endDatetime) {
        List<Ga4FilteredDto.Response> responseDtoList = ga4FilteredService.findGa4FilteredAll(startDatetime, endDatetime);
        return ResponseData.toResponseEntity(ResponseCode.READ_GA4FILTERED, responseDtoList);
    }

    @GetMapping("/ga4/calc-data")
    @Operation(summary = "GA4 계산용 데이터 조회 [JWT O]",
            description = """
                    <strong>< RequestParam ></strong>
                    - <strong>startDatetime, endDatetime</strong> : "2025-08-29 23:59:59" 형태의 날짜시각  \n
                    <strong>< URI ></strong>
                    - <strong>예시 URI</strong> : /back-office/ga4/calc-data?startDatetime=2025-08-01%2015:30:00&endDatetime=2025-08-29%2023:59:59
                    - <strong>참고 사항</strong> : 중간의 '%20'은 공백을 의미
                    """)
    public ResponseEntity<ResponseData<List<Ga4FilteredDto.CalcResponse>>> findGa4FilteredCalc(
            @RequestParam(value = "startDatetime", required = true) String startDatetime,
            @RequestParam(value = "endDatetime", required = true) String endDatetime) {
        List<Ga4FilteredDto.CalcResponse> calcResponseDtoList = ga4FilteredService.findGa4FilteredCalc(startDatetime, endDatetime);
        return ResponseData.toResponseEntity(ResponseCode.READ_GA4FILTERED, calcResponseDtoList);
    }

    @GetMapping("/ga4/statistics")
    @Operation(summary = "페이지별 이용자 통계 조회 [JWT O]",
            description = """
                    <strong>< RequestParam ></strong>
                    - <strong>startDatetime, endDatetime</strong> : "2025-08-29 23:59:59" 형태의 날짜시각  \n
                    <strong>< URI ></strong>
                    - <strong>예시 URI</strong> : /back-office/ga4/statistics?startDatetime=2025-08-01%2015:30:00&endDatetime=2025-08-29%2023:59:59
                    - <strong>참고 사항</strong> : 중간의 '%20'은 공백을 의미
                    """)
    public ResponseEntity<ResponseData<List<Ga4FilteredDto.StatisticResponse>>> calculateStatistic(
            @RequestParam(value = "startDatetime", required = true) String startDatetime,
            @RequestParam(value = "endDatetime", required = true) String endDatetime) {
        List<Ga4FilteredDto.StatisticResponse> statisticResponseDtoList = ga4FilteredService.calculateStatistic(startDatetime, endDatetime);
        return ResponseData.toResponseEntity(ResponseCode.READ_GA4FILTERED, statisticResponseDtoList);
    }

    @GetMapping("/users/statistics")
    @Operation(summary = "회원 수 통계 조회 [JWT O]")
    public ResponseEntity<ResponseData<UserDto.CountResponse>> countUsers() {
        UserDto.CountResponse countResponseDto = userService.countUsers();
        return ResponseData.toResponseEntity(ResponseCode.READ_USER, countResponseDto);
    }

    @GetMapping("/memory/heap")
    @Operation(summary = "힙메모리 사용량 조회 [JWT O]")
    public ResponseEntity<ResponseData<Map<String, String>>> getHeapMemoryUsage() {
        Map<String, String> heapMemoryMap = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();

        double limitMaxMB = runtime.maxMemory() / MB_DIVISOR;  // 설정된 JVM 한도의 최대 힙메모리 (한계치)
        double currentMaxMB = runtime.totalMemory() / MB_DIVISOR;  // 현재 OS에서 할당받은 최대 힙메모리 (점점 늘어남)
        double usedMB = currentMaxMB - (runtime.freeMemory() / MB_DIVISOR);
        double remainMB = limitMaxMB - usedMB;
        double usedPercent = usedMB * 100 / limitMaxMB;
        usedPercent = Math.round(usedPercent * 100) / 100.0;

        heapMemoryMap.put("maxHeap", String.format("100%% (%.2fMB · %.2fGB)", limitMaxMB, limitMaxMB/1024));  // 최대
        heapMemoryMap.put("usedHeap", String.format("%.2f%% (%.2fMB · %.2fGB)", usedPercent, usedMB, usedMB/1024));  // 사용
        heapMemoryMap.put("remainHeap", String.format("%.2f%% (%.2fMB · %.2fGB)", remainMB*100/limitMaxMB, remainMB, remainMB/1024));  // 잔여

        return ResponseData.toResponseEntity(ResponseCode.READ_MEMORY, heapMemoryMap);
    }
}
