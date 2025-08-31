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

import java.util.List;

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

    @GetMapping("/users/statistics")
    @Operation(summary = "회원 수 통계 조회 [JWT O]")
    public ResponseEntity<ResponseData<UserDto.CountResponse>> countUsers() {
        UserDto.CountResponse countResponseDto = userService.countUsers();
        return ResponseData.toResponseEntity(ResponseCode.READ_USER, countResponseDto);
    }
}
