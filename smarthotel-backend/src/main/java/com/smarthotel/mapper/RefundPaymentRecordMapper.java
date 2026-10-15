package com.smarthotel.mapper;

import com.smarthotel.entity.RefundPaymentRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RefundPaymentRecordMapper {

    @Insert("""
        insert into refund_payment_record
        (refund_id, order_id, refund_no, refund_amount, refund_method,
         refund_status, inventory_sync_status, refunded_time)
        values (#{refundId}, #{orderId}, #{refundNo}, #{refundAmount},
                #{refundMethod}, #{refundStatus}, #{inventorySyncStatus}, #{refundedTime})
    """)
    int insert(RefundPaymentRecord record);

    @Select("select * from refund_payment_record where refund_id = #{refundId}")
    RefundPaymentRecord selectByRefundId(Long refundId);

    @Select("""
        select * from refund_payment_record
        where inventory_sync_status = 'PENDING'
        order by refund_payment_id
        limit 100
    """)
    List<RefundPaymentRecord> selectPendingInventorySync();

    @Update("""
        update refund_payment_record set inventory_sync_status = 'SYNCED'
        where refund_id = #{refundId} and inventory_sync_status = 'PENDING'
    """)
    int markInventorySynced(Long refundId);
}
