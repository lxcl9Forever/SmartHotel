package com.smarthotel.mapper;

import com.smarthotel.vo.AdminPaymentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AdminPaymentQueryMapper {
    @Select("select count(*) from payment")
    long countAll();

    @Select("""
        select p.payment_id as paymentId, p.payment_no as paymentNo,
               p.order_id as orderId, o.order_no as orderNo,
               p.payment_method as paymentMethod, p.amount,
               p.payment_status as paymentStatus, p.third_party_no as thirdPartyNo,
               p.paid_time as paidTime
        from payment p join booking_order o on o.order_id = p.order_id
        order by p.payment_id desc limit #{offset}, #{pageSize}
        """)
    List<AdminPaymentVO> list(@Param("offset") long offset, @Param("pageSize") int pageSize);
}
