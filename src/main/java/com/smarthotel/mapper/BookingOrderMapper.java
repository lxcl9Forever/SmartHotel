package com.smarthotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthotel.entity.BookingOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookingOrderMapper
        extends BaseMapper<BookingOrder> {
}