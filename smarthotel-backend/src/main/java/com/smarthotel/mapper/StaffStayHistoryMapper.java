package com.smarthotel.mapper;

import com.smarthotel.vo.StaffStayHistoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface StaffStayHistoryMapper {
    @Select("select count(*) from check_in_record")
    long countAll();

    @Select("""
        select ci.check_in_id as checkInId, o.order_no as orderNo,
               o.contact_name as contactName, r.room_no as roomNo, r.floor_no as floorNo,
               rt.type_name as roomTypeName, ci.actual_check_in_time as actualCheckInTime,
               co.actual_check_out_time as actualCheckOutTime,
               u1.username as checkInStaff, u2.username as checkOutStaff
        from check_in_record ci
        join room_assignment a on a.assignment_id = ci.assignment_id
        join booking_order o on o.order_id = a.order_id
        join room r on r.room_id = a.room_id
        join room_type rt on rt.room_type_id = r.room_type_id
        left join check_out_record co on co.check_in_id = ci.check_in_id
        left join sys_user u1 on u1.user_id = ci.handled_by
        left join sys_user u2 on u2.user_id = co.handled_by
        order by ci.actual_check_in_time desc, ci.check_in_id desc
        limit #{offset}, #{pageSize}
        """)
    List<StaffStayHistoryVO> list(@Param("offset") long offset, @Param("pageSize") int pageSize);
}
