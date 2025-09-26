package com.shj.onlinememospringproject.service.impl;

import com.shj.onlinememospringproject.client.Ga4Client;
import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import com.shj.onlinememospringproject.dto.Ga4FilteredDto;
import com.shj.onlinememospringproject.repository.Ga4FilteredBatchRepository;
import com.shj.onlinememospringproject.repository.Ga4FilteredRepository;
import com.shj.onlinememospringproject.response.exception.Exception400;
import com.shj.onlinememospringproject.response.exception.Exception500;
import com.shj.onlinememospringproject.service.Ga4FilteredService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Ga4FilteredServiceImpl implements Ga4FilteredService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String ALL_PAGE_ALIAS = "전체 페이지 합계";
    private static final String AUTH_PAGE_ALIAS = "회원용 페이지 합계";
    private static final String PUBLIC_PAGE_ALIAS = "공개 페이지 합계";
    private static final Set<String> AUTH_PAGE_SET = Set.of("/users", "/friends", "/senders", "/memos", "/memos/:memoId", "/memos/new-memo");  // 회원용 페이지
    private static final Set<String> PUBLIC_PAGE_SET = Set.of("/", "/signup", "/password", "/information", "/notice", "/download", "/404");  // 공개 페이지

    private final Ga4FilteredRepository ga4FilteredRepository;
    private final Ga4FilteredBatchRepository ga4FilteredBatchRepository;
    private final Ga4Client ga4Client;

    @Value("${feignclient.ga4.auth-token}")
    private String authToken;


    @Transactional
    @Override
    public void filterAndSaveGa4(String startDatetimeStr, String endDatetimeStr) {  // KST 기준 파라미터
        checkValidDatetime(startDatetimeStr, endDatetimeStr);

        // filter from BigQuery
        ResponseEntity<List<Ga4FilteredDto.ClientResponse>> responseEntity;
        try {
            responseEntity = ga4Client.filterFromBigQuery(authToken, startDatetimeStr, endDatetimeStr);
        } catch (Exception ex) {
            throw new Exception500.ExternalServer(String.format("Ga4Client API 호출 에러 (%s)", ex.getMessage()));
        }
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

        LocalDateTime startDatetime = convertStrToDatetime(startDatetimeStr);
        LocalDateTime endDatetime = convertStrToDatetime(endDatetimeStr);
        List<Ga4Filtered> ga4FilteredList = ga4FilteredRepository.findByEventDatetimeBetweenOrderByEventDatetimeAsc(startDatetime, endDatetime);

        List<Ga4FilteredDto.Response> responseDtoList = ga4FilteredList.stream()
                .map(Ga4FilteredDto.Response::new)
                .collect(Collectors.toList());
        return responseDtoList;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Ga4FilteredDto.CalcResponse> findGa4FilteredCalc(String startDatetimeStr, String endDatetimeStr) {  // KST 기준 파라미터
        checkValidDatetime(startDatetimeStr, endDatetimeStr);

        LocalDateTime startDatetime = convertStrToDatetime(startDatetimeStr);
        LocalDateTime endDatetime = convertStrToDatetime(endDatetimeStr);
        List<Ga4Filtered> ga4FilteredList = ga4FilteredRepository.findByEventDatetimeBetweenOrderByEventDatetimeAsc(startDatetime, endDatetime);

        List<Ga4FilteredDto.CalcResponse> calcResponseDtoList = ga4FilteredList.stream()
                .map(Ga4FilteredDto.CalcResponse::new)
                .collect(Collectors.toList());
        return calcResponseDtoList;
    }

    @Transactional(readOnly = true)
    @Override
    public List<Ga4FilteredDto.StatisticResponse> calculateStatistic(String startDatetimeStr, String endDatetimeStr) {
        List<Ga4FilteredDto.CalcResponse> calcResponseDtoList = findGa4FilteredCalc(startDatetimeStr, endDatetimeStr);  // checkValidDatetime() 검사 포함됨

        // 로그인한 사용자들의 pseudoId (전역 범위 : 실사용자 수 집계 시, 활성 사용자 중복 제거용)
        Set<String> pseudoIdWithLoginSet = calcResponseDtoList.stream()
                .filter(calcResponseDto -> calcResponseDto.getLoginUserId() != null && calcResponseDto.getLoginUserId() > 0)
                .map(Ga4FilteredDto.CalcResponse::getUserPseudoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 페이지별 그룹화
        Map<String, List<Ga4FilteredDto.CalcResponse>> pageMap = calcResponseDtoList.stream()
                .collect(Collectors.groupingBy(Ga4FilteredDto.CalcResponse::getPagePath));
        List<Ga4FilteredDto.CalcResponse> authEventDataList = new ArrayList<>();
        List<Ga4FilteredDto.CalcResponse> publicEventDataList = new ArrayList<>();

        // [ 각 페이지별 통계 ]
        List<Ga4FilteredDto.StatisticResponse> eachStatisticResponseDtoList = new ArrayList<>();
        for(Map.Entry<String, List<Ga4FilteredDto.CalcResponse>> entry : pageMap.entrySet()) {
            String pagePath = entry.getKey();
            List<Ga4FilteredDto.CalcResponse> eventDataList = entry.getValue();

            if(PUBLIC_PAGE_SET.contains(pagePath)) {
                publicEventDataList.addAll(eventDataList);
            }
            else if(AUTH_PAGE_SET.contains(pagePath)) {
                authEventDataList.addAll(eventDataList);
            }

            eachStatisticResponseDtoList.add(
                    calculateValues(pagePath, eventDataList, pseudoIdWithLoginSet)
            );
        }

        // [ 전체 페이지 통계 ]
        List<Ga4FilteredDto.StatisticResponse> statisticResponseDtoList = new ArrayList<>();
        statisticResponseDtoList.add(
                calculateValues(ALL_PAGE_ALIAS, calcResponseDtoList, pseudoIdWithLoginSet)
        );

        // [ 회원용 페이지 통계 ]
        statisticResponseDtoList.add(
                calculateValues(AUTH_PAGE_ALIAS, authEventDataList, pseudoIdWithLoginSet)
        );

        // [ 공개 페이지 통계 ]
        statisticResponseDtoList.add(
                calculateValues(PUBLIC_PAGE_ALIAS, publicEventDataList, pseudoIdWithLoginSet)
        );

        // 통계 정렬
        eachStatisticResponseDtoList.sort(
                Comparator.comparingLong(Ga4FilteredDto.StatisticResponse::getUniqueUserCount).reversed()  // 정렬 우선순위 1: '실사용자 수' 내림차순
                        .thenComparingLong(Ga4FilteredDto.StatisticResponse::getLoginUserCount).reversed()  // 정렬 우선순위 2: '로그인 사용자 수' 내림차순
                        .thenComparingLong(Ga4FilteredDto.StatisticResponse::getActiveUserCount).reversed()  // 정렬 우선순위 3: '활성 사용자 수' 내림차순
                        .thenComparingLong(Ga4FilteredDto.StatisticResponse::getPageViewCount).reversed()  // 정렬 우선순위 4: '조회수' 내림차순
                        .thenComparingLong(Ga4FilteredDto.StatisticResponse::getUnauthBlockedCount).reversed()  // 정렬 우선순위 5: '미인증 접근 차단' 내림차순
                        .thenComparing(Ga4FilteredDto.StatisticResponse::getPagePath)  // 정렬 우선순위 6: '페이지 경로' 오름차순
        );
        statisticResponseDtoList.addAll(eachStatisticResponseDtoList);

        return statisticResponseDtoList;
    }

    public Ga4FilteredDto.StatisticResponse calculateValues(String pagePath, List<Ga4FilteredDto.CalcResponse> eventDataList, Set<String> pseudoIdWithLoginSet) {
        if(eventDataList == null || eventDataList.isEmpty()) {
            return Ga4FilteredDto.StatisticResponse.builder()
                    .pagePath(pagePath)
                    .uniqueUserCount(0L)
                    .loginUserCount(0L)
                    .activeUserCount(0L)
                    .pageViewCount(0L)
                    .unauthBlockedCount(0L)
                    .build();
        }

        // 로그인 사용자 수
        Set<Long> loginUserIdSet = eventDataList.stream()
                .map(Ga4FilteredDto.CalcResponse::getLoginUserId)
                .filter(loginUserId -> loginUserId != null && loginUserId > 0)
                .collect(Collectors.toSet());
        long loginUserCount = loginUserIdSet.size();

        // 실사용자 수
        long uniqueUserCount = loginUserCount;
        if(pagePath.equals(ALL_PAGE_ALIAS) || pagePath.equals(PUBLIC_PAGE_ALIAS) || PUBLIC_PAGE_SET.contains(pagePath)) {
            Set<String> pseudoIdSet = eventDataList.stream()
                    .filter(calcResponseDto -> calcResponseDto.getLoginUserId() != null && calcResponseDto.getLoginUserId() == 0)
                    .map(Ga4FilteredDto.CalcResponse::getUserPseudoId)
                    .filter(pseudoId -> pseudoId != null && !pseudoIdWithLoginSet.contains(pseudoId))
                    .collect(Collectors.toSet());
            uniqueUserCount += pseudoIdSet.size();
        }

        // 활성 사용자 수
        long activeUserCount = eventDataList.stream()
                .map(Ga4FilteredDto.CalcResponse::getUserPseudoId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // 조회수
        long pageViewCount = eventDataList.size();

        // 미인증 접근 차단
        long unauthBlockedCount = eventDataList.stream()
                .filter(calcResponseDto -> calcResponseDto.getLoginUserId() != null && calcResponseDto.getLoginUserId() == -1)
                .count();

        return Ga4FilteredDto.StatisticResponse.builder()
                .pagePath(pagePath)
                .uniqueUserCount(uniqueUserCount)
                .loginUserCount(loginUserCount)
                .activeUserCount(activeUserCount)
                .pageViewCount(pageViewCount)
                .unauthBlockedCount(unauthBlockedCount)
                .build();
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

    private static LocalDateTime convertStrToDatetime(String datetimeStr) {  // KST 기준 파라미터
        if(datetimeStr == null || datetimeStr.isEmpty()) {
            throw new Exception400.Ga4FilteredBadRequest("변환할 날짜 문자열이 비어있습니다.");
        }

        LocalDateTime datetimeKst = LocalDateTime.parse(datetimeStr, FORMATTER);
        return datetimeKst;

        // 주의 : 검색 시 Spring Data MongoDB가 자동으로 KST -> UTC로 변환해 쿼리를 전송하므로, 추가 변환하지 말것.
//        LocalDateTime datetimeUtc = datetimeKst.minusHours(9);
//        return datetimeUtc;
    }
}
