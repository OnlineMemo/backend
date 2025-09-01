package com.shj.onlinememospringproject.client;

import com.shj.onlinememospringproject.config.FeignConfig;
import com.shj.onlinememospringproject.dto.Ga4FilteredDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// GA4 (React: 실사용자 지표 수집)
// -> BigQuery (SQL: 수집 데이터 검증)
// -> Cloud Run (Node.js Serverless API: 데이터 필터링 및 제공)
// -> MongoDB (Spring: 필터링된 지표 저장 및 백오피스 운용)
@FeignClient(name = "ga4", url = "${feignclient.ga4.url}", configuration = FeignConfig.class)
public interface Ga4Client {

    @GetMapping(value = "/filtered")
    ResponseEntity<List<Ga4FilteredDto.ClientResponse>> filterFromBigQuery(
            @RequestHeader("auth-token") String authToken,
            @RequestParam("startDatetime") String startDatetimeStr,
            @RequestParam("endDatetime") String endDatetimeStr
    );
}
