package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.domain.backoffice.Ga4Filtered;
import com.shj.onlinememospringproject.dto.Ga4FilteredDto;

import java.util.List;

public interface Ga4FilteredService {
    void filterAndSaveGa4(String startDatetimeStr, String endDatetimeStr);
    List<Ga4FilteredDto.Response> findGa4FilteredAll(String startDatetimeStr, String endDatetimeStr, boolean excludeDdos);
    List<Ga4FilteredDto.CalcResponse> findGa4FilteredCalc(String startDatetimeStr, String endDatetimeStr, boolean excludeDdos);
    List<Ga4FilteredDto.StatisticResponse> calculateStatistic(String startDatetimeStr, String endDatetimeStr, boolean excludeDdos);
    List<Ga4FilteredDto.StatisticResponse> calculateStatistic(List<Ga4FilteredDto.CalcResponse> calcResponseDtoList);  // Overloading 메소드 (for 단일 책임 원칙)
    Ga4FilteredDto.AnalyzeResponse analyzeFacade(String startDatetimeStr, String endDatetimeStr, boolean excludeDdos);

    // ========== 유틸성 메소드 ========== //
    List<Ga4Filtered> findGa4FilteredByDatetime(String startDatetimeStr, String endDatetimeStr);
}
