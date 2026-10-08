package com.smarthotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthotel.entity.HotelDailyStatistics;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HotelDailyStatisticsMapper
        extends BaseMapper<HotelDailyStatistics> {
}