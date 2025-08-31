package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.client.Ga4Client;
import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import com.shj.onlinememospringproject.dto.Ga4FilteredDto;
import com.shj.onlinememospringproject.repository.Ga4FilteredBatchRepository;
import com.shj.onlinememospringproject.repository.Ga4FilteredRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.service.Ga4FilteredService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Ga4FilteredServiceImpl implements Ga4FilteredService {

    private final Ga4FilteredRepository ga4FilteredRepository;
    private final Ga4FilteredBatchRepository ga4FilteredBatchRepository;
    private final Ga4Client ga4Client;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${feignclient.ga4.auth-token}")
    private String authToken;


    @Transactional
    @Override
    public void filterAndSaveGa4(String startDatetimeStr, String endDatetimeStr) {  // KST 기준 파라미터
        checkValidDatetime(startDatetimeStr, endDatetimeStr);

        // filter from BigQuery
        ResponseEntity<List<Ga4FilteredDto.ClientResponse>> responseEntity = ga4Client.filterFromBigQuery(authToken, startDatetimeStr, endDatetimeStr);
        List<Ga4FilteredDto.ClientResponse> clientResponseDtoList = responseEntity.getBody();

        // save to MongoDB
        List<Ga4Filtered> ga4FilteredList = clientResponseDtoList.stream()
                .map(clientResponseDto -> Ga4Filtered.Ga4FilteredSaveBuilder()
                        .eventDatetime(clientResponseDto.getEvent_datetime_kst())
                        .userPseudoId(clientResponseDto.getUser_pseudo_id())
                        .loginUserId(clientResponseDto.getLogin_user_id())
                        .pageTitle(clientResponseDto.getPage_title())
                        .pageReferrer(clientResponseDto.getPage_referrer())
                        .pageLocation(clientResponseDto.getPage_location())
                        .pagePath(clientResponseDto.getPage_path())
                        .deviceCategory(clientResponseDto.getDevice_category())
                        .deviceBrand(clientResponseDto.getDevice_brand())
                        .deviceBrowser(clientResponseDto.getDevice_browser())
                        .geoCountry(clientResponseDto.getGeo_country())
                        .geoRegion(clientResponseDto.getGeo_region())
                        .geoCity(clientResponseDto.getGeo_city())
                        .build()
                )
                .collect(Collectors.toList());

        ga4FilteredBatchRepository.batchInsert(ga4FilteredList);  // Ga4Filtered - Batch Insert
    }

    @Transactional(readOnly = true)
    @Override
    public List<Ga4FilteredDto.Response> findGa4FilteredAll(String startDatetimeStr, String endDatetimeStr) {  // KST 기준 파라미터
        checkValidDatetime(startDatetimeStr, endDatetimeStr);

        // MongoDB 검색 파라미터용 KST -> UTC 변환
        LocalDateTime startDatetimeUtc = convertToDatetimeUtc(startDatetimeStr);
        LocalDateTime endDatetimeUtc = convertToDatetimeUtc(endDatetimeStr);
        List<Ga4Filtered> ga4FilteredList = ga4FilteredRepository.findByEventDatetimeBetweenOrderByEventDatetimeAsc(startDatetimeUtc, endDatetimeUtc);

        List<Ga4FilteredDto.Response> responseDtoList = ga4FilteredList.stream()
                .map(Ga4FilteredDto.Response::new)
                .collect(Collectors.toList());
        return responseDtoList;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Ga4FilteredDto.CalcResponse> findGa4FilteredCalc(String startDatetimeStr, String endDatetimeStr) {  // KST 기준 파라미터
        checkValidDatetime(startDatetimeStr, endDatetimeStr);

        // MongoDB 검색 파라미터용 KST -> UTC 변환
        LocalDateTime startDatetimeUtc = convertToDatetimeUtc(startDatetimeStr);
        LocalDateTime endDatetimeUtc = convertToDatetimeUtc(endDatetimeStr);
        List<Ga4Filtered> ga4FilteredList = ga4FilteredRepository.findByEventDatetimeBetweenOrderByEventDatetimeAsc(startDatetimeUtc, endDatetimeUtc);

        List<Ga4FilteredDto.CalcResponse> calcResponseDtoList = ga4FilteredList.stream()
                .map(Ga4FilteredDto.CalcResponse::new)
                .collect(Collectors.toList());
        return calcResponseDtoList;
    }


    // ========== 유틸성 메소드 ========== //

    private static void checkValidDatetime(String startDatetimeStr, String endDatetimeStr) {
        if(startDatetimeStr.compareTo(endDatetimeStr) > 0) {
            throw new Exception400.Ga4FilteredBadRequest("날짜 시작일은 종료일보다 이후일 수 없습니다.");
        }

        try {
            LocalDateTime.parse(startDatetimeStr, FORMATTER);
            LocalDateTime.parse(endDatetimeStr, FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new Exception400.Ga4FilteredBadRequest("잘못된 날짜 형식으로 API를 요청하였습니다.");
        }
    }

    private static LocalDateTime convertToDatetimeUtc(String datetimeStr) {  // KST 기준 파라미터
        if(datetimeStr == null || datetimeStr.isEmpty()) {
            throw new Exception400.Ga4FilteredBadRequest("변환할 날짜 문자열이 비어있습니다.");
        }
        LocalDateTime datetimeKst = LocalDateTime.parse(datetimeStr, FORMATTER);
        LocalDateTime datetimeUtc = datetimeKst.minusHours(9);

        return datetimeUtc;
    }
}
