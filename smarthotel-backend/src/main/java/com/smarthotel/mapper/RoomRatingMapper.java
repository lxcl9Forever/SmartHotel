package com.smarthotel.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface RoomRatingMapper {
    @Select("""
            SELECT AVG(r.rating)
            FROM order_review r
            JOIN booking_order o ON o.order_id = r.order_id
            WHERE o.room_type_id = #{roomTypeId} AND r.status = 'VISIBLE'
            """)
    BigDecimal averageRating(Long roomTypeId);
}
