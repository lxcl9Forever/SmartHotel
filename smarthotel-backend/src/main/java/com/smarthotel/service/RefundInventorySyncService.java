package com.smarthotel.service;

import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.RefundPaymentRecord;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.RefundPaymentRecordMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundInventorySyncService {

    private static final Logger log = LoggerFactory.getLogger(RefundInventorySyncService.class);

    private final RefundPaymentRecordMapper refundPaymentRecordMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final InventoryRedisService inventoryRedisService;

    public void syncOne(Long refundId) {
        try {
            RefundPaymentRecord record = refundPaymentRecordMapper.selectByRefundId(refundId);
            if (record == null || !"PENDING".equals(record.getInventorySyncStatus())) {
                return;
            }
            BookingOrder order = bookingOrderMapper.selectById(record.getOrderId());
            if (order == null) {
                log.error("Refund {} has no order for inventory synchronization", refundId);
                return;
            }
            Long result = inventoryRedisService.releaseRefundedInventoryOnce(
                    refundId, order.getRoomTypeId(), order.getCheckInDate(),
                    order.getCheckOutDate(), order.getRoomCount());
            if (Long.valueOf(1L).equals(result)) {
                refundPaymentRecordMapper.markInventorySynced(refundId);
            } else {
                log.warn("Refund {} inventory Redis synchronization pending: {}", refundId, result);
            }
        } catch (RuntimeException error) {
            // The database refund has committed. A later scheduled run retries safely.
            log.warn("Refund {} inventory Redis synchronization will be retried", refundId, error);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void retryPending() {
        try {
            for (RefundPaymentRecord record : refundPaymentRecordMapper.selectPendingInventorySync()) {
                syncOne(record.getRefundId());
            }
        } catch (RuntimeException error) {
            // Before migration or during a database outage, retry on the next run.
            log.warn("Unable to load pending refund inventory synchronizations", error);
        }
    }
}
