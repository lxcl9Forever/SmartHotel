package com.smarthotel.service;

import com.smarthotel.entity.HotelDailyStatistics;

import java.time.LocalDate;
import java.util.List;

public interface AdminStatisticsService {

    void refreshDailyStatistics(
            LocalDate date
    );

    void refreshStatisticsRange(
            LocalDate startDate,
            LocalDate endDate
    );

    List<HotelDailyStatistics>
    listDailyStatistics(
            LocalDate startDate,
            LocalDate endDate
    );
}