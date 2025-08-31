package com.shj.onlinememospringproject.service;

import com.shj.onlinememospringproject.dto.Ga4FilteredDto;

import java.util.List;

public interface Ga4FilteredService {
    void filterAndSaveGa4(String startDatetimeStr, String endDatetimeStr);
    List<Ga4FilteredDto.Response> findGa4FilteredAll(String startDatetimeStr, String endDatetimeStr);
    List<Ga4FilteredDto.CalcResponse> findGa4FilteredCalc(String startDatetimeStr, String endDatetimeStr);
    List<Ga4FilteredDto.StatisticResponse> calculateStatistic(String startDatetimeStr, String endDatetimeStr);
}
