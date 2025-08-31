package com.shj.onlinememospringproject.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import com.shj.onlinememospringproject.util.TimeConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class Ga4FilteredDto {

    // ======== < Request DTO > ======== //


    // ======== < Response DTO > ======== //

    @Getter
    @NoArgsConstructor
    public static class Response {

        private LocalDateTime eventDatetime;
        private String eventDatetimeStr;

        private String userPseudoId;
        private Long loginUserId;

        private String pageTitle;
        private String pageReferrer;
        private String pageLocation;
        private String pagePath;

        private String deviceCategory;
        private String deviceBrand;
        private String deviceBrowser;

        private String geoCountry;
        private String geoRegion;
        private String geoCity;

        public Response(Ga4Filtered entity) {
            this.eventDatetime = entity.getEventDatetime();
            this.eventDatetimeStr = TimeConverter.timeToString(entity.getEventDatetime());
            this.userPseudoId = entity.getUserPseudoId();
            this.loginUserId = entity.getLoginUserId();
            this.pageTitle = entity.getPageTitle();
            this.pageReferrer = entity.getPageReferrer();
            this.pageLocation = entity.getPageLocation();
            this.pagePath = entity.getPagePath();
            this.deviceCategory = entity.getDeviceCategory();
            this.deviceBrand = entity.getDeviceBrand();
            this.deviceBrowser = entity.getDeviceBrowser();
            this.geoCountry = entity.getGeoCountry();
            this.geoRegion = entity.getGeoRegion();
            this.geoCity = entity.getGeoCity();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class CalcResponse {

        private LocalDateTime eventDatetime;

        private String userPseudoId;
        private Long loginUserId;

        private String pagePath;

        public CalcResponse(Ga4Filtered entity) {
            this.eventDatetime = entity.getEventDatetime();
            this.userPseudoId = entity.getUserPseudoId();
            this.loginUserId = entity.getLoginUserId();
            this.pagePath = entity.getPagePath();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ClientResponse {

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        private LocalDateTime event_datetime_kst;

        private String user_pseudo_id;
        private Long login_user_id;

        private String page_title;
        private String page_referrer;
        private String page_location;
        private String page_path;

        private String device_category;
        private String device_brand;
        private String device_browser;

        private String geo_country;
        private String geo_region;
        private String geo_city;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticResponse {

        private String pagePath;  // 페이지 경로
        private long uniqueUserCount;  // 실사용자 수
        private long loginUserCount;  // 로그인 사용자 수
        private long activeUserCount;  // 활성 사용자 수
        private long pageViewCount;  // 조회수
        private long unauthBlockedCount;  // 미인증 접근 차단
    }
}
