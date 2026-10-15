package com.smarthotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthotel.entity.RoomPrice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomPriceMapper
        extends BaseMapper<RoomPrice> {
}