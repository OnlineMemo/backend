package com.shj.onlinememospringproject.domain.backoffice;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor

@Document(collection = "ga4_filtered")  // 백오피스 전용 데이터는 MongoDB로 관리.
public class Ga4Filtered {

    @Id
    private String id;

    @Indexed
    @Field("event_datetime")
    private LocalDateTime eventDatetime;  // Spring 저장값: KST, MongoDB 자동 저장값: UTC, MongoDB 검색파라미터: KST, Spring 조회결과: KST

    @Field("user_pseudo_id")
    private String userPseudoId;
    @Field("login_user_id")
    private Long loginUserId;

    @Field("page_title")
    private String pageTitle;
    @Field("page_referrer")
    private String pageReferrer;
    @Field("page_location")
    private String pageLocation;
    @Field("page_path")
    private String pagePath;

    @Field("device_category")
    private String deviceCategory;
    @Field("device_brand")
    private String deviceBrand;
    @Field("device_browser")
    private String deviceBrowser;

    @Field("geo_country")
    private String geoCountry;
    @Field("geo_region")
    private String geoRegion;
    @Field("geo_city")
    private String geoCity;


    @Builder(builderClassName = "Ga4FilteredSaveBuilder", builderMethodName = "Ga4FilteredSaveBuilder")
    public Ga4Filtered(
            LocalDateTime eventDatetime,
            String userPseudoId, Long loginUserId,
            String pageTitle, String pageReferrer, String pageLocation, String pagePath,
            String deviceCategory, String deviceBrand, String deviceBrowser,
            String geoCountry, String geoRegion, String geoCity
    ) {
        // 이 빌더는 Ga4Filtered 생성때만 사용할 용도
        this.eventDatetime = eventDatetime;
        this.userPseudoId = userPseudoId;
        this.loginUserId = loginUserId;
        this.pageTitle = pageTitle;
        this.pageReferrer = pageReferrer;
        this.pageLocation = pageLocation;
        this.pagePath = pagePath;
        this.deviceCategory = deviceCategory;
        this.deviceBrand = deviceBrand;
        this.deviceBrowser = deviceBrowser;
        this.geoCountry = geoCountry;
        this.geoRegion = geoRegion;
        this.geoCity = geoCity;
    }
}
