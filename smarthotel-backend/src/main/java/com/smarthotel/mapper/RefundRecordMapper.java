package com.smarthotel.mapper;

import com.smarthotel.entity.RefundRecord;
import com.smarthotel.vo.AdminRefundVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RefundRecordMapper {


    @Insert("""
        insert into refund_record
        (
            order_id,
            user_id,
            refund_amount,
            refund_reason,
            refund_status
        )
        values
        (
            #{orderId},
            #{userId},
            #{refundAmount},
            #{refundReason},
            #{refundStatus}
        )
    """)
    void insert(RefundRecord refundRecord);



    @Select("""
        select *
        from refund_record
        where order_id = #{orderId}
        order by refund_id desc
        limit 1
    """)
    RefundRecord selectByOrderId(Long orderId);

    @Select("select * from refund_record where refund_id = #{refundId}")
    RefundRecord selectById(Long refundId);

    @Select("select * from refund_record where refund_id = #{refundId} for update")
    RefundRecord selectByIdForUpdate(Long refundId);

    @Select("""
        <script>
        select count(*) from refund_record
        <if test="status != null">where refund_status = #{status}</if>
        </script>
    """)
    long countForAdmin(@Param("status") String status);

    @Select("""
        <script>
        select r.refund_id, r.order_id, o.order_no, r.user_id,
               u.username, o.contact_name, r.refund_amount,
               r.refund_reason, r.refund_status, r.create_time,
               rp.refund_no, rp.refunded_time, rp.inventory_sync_status
        from refund_record r
        join booking_order o on o.order_id = r.order_id
        join sys_user u on u.user_id = r.user_id
        left join refund_payment_record rp on rp.refund_id = r.refund_id
        <if test="status != null">where r.refund_status = #{status}</if>
        order by r.create_time desc, r.refund_id desc
        limit #{offset}, #{pageSize}
        </script>
    """)
    List<AdminRefundVO> listForAdmin(
            @Param("status") String status,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize
    );



    @Update("""
        update refund_record
        set refund_status=#{refundStatus}
        where refund_id=#{refundId} and refund_status='PENDING'
    """)
    int updateStatus(
            @Param("refundId") Long refundId,
            @Param("refundStatus") String refundStatus
    );

}
