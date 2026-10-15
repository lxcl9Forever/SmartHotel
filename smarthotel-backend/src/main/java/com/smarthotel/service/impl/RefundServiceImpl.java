package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.Customer;
import com.smarthotel.entity.RefundRecord;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.RefundRecordMapper;
import com.smarthotel.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRecordMapper refundRecordMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final CustomerMapper customerMapper;

    @Override
    @Transactional
    public void applyRefund(Long orderId, Long userId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(400, "请输入退款原因");
        }

        // Lock the order so concurrent requests cannot create two refund records.
        BookingOrder order = bookingOrderMapper.selectOne(
                new LambdaQueryWrapper<BookingOrder>()
                        .eq(BookingOrder::getOrderId, orderId)
                        .last("FOR UPDATE")
        );
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getUserId, userId)
        );
        if (customer == null || !order.getCustomerId().equals(customer.getCustomerId())) {
            throw new BusinessException(403, "无权操作该订单");
        }

        RefundRecord existing = refundRecordMapper.selectByOrderId(orderId);
        if (existing != null && !"REJECTED".equals(existing.getRefundStatus())
                && !"REJECT".equals(existing.getRefundStatus())) {
            throw new BusinessException(409, "该订单已经申请退款，请勿重复申请");
        }
        if (!"CONFIRMED".equals(order.getOrderStatus())
                || !"PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException(409, "当前订单状态不能申请退款");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0) {
            throw new BusinessException(409, "该订单由优惠券全额抵扣，没有可退还的支付金额");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0) {
            throw new BusinessException(409, "该订单由优惠券全额抵扣，没有可退还的支付金额");
        }

        RefundRecord record = new RefundRecord();
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setRefundAmount(order.getTotalAmount());
        record.setRefundReason(reason.trim());
        record.setRefundStatus("PENDING");
        refundRecordMapper.insert(record);

        order.setPaymentStatus("REFUNDING");
        bookingOrderMapper.updateById(order);
    }
}
