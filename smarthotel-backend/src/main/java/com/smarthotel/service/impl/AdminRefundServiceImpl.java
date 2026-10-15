package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.Payment;
import com.smarthotel.entity.RefundRecord;
import com.smarthotel.entity.RefundPaymentRecord;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.PaymentMapper;
import com.smarthotel.mapper.RefundRecordMapper;
import com.smarthotel.mapper.RefundPaymentRecordMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.service.AdminRefundService;
import com.smarthotel.vo.AdminRefundVO;
import com.smarthotel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminRefundServiceImpl implements AdminRefundService {

    private static final Set<String> STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

    private final RefundRecordMapper refundRecordMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final PaymentMapper paymentMapper;
    private final RoomInventoryMapper roomInventoryMapper;
    private final RefundPaymentRecordMapper refundPaymentRecordMapper;

    @Override
    public PageResult<AdminRefundVO> listRefunds(String status, Integer page, Integer pageSize) {
        String filter = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (filter != null && !STATUSES.contains(filter)) {
            throw new BusinessException(400, "无效的退款状态");
        }

        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        long total = refundRecordMapper.countForAdmin(filter);
        List<AdminRefundVO> records = total == 0
                ? Collections.emptyList()
                : refundRecordMapper.listForAdmin(
                        filter, (long) (safePage - 1) * safePageSize, safePageSize);
        return new PageResult<>(total, safePage, safePageSize, records);
    }

    @Override
    @Transactional
    public void approve(Long refundId) {
        review(refundId, true);
    }

    @Override
    @Transactional
    public void reject(Long refundId) {
        review(refundId, false);
    }

    private void review(Long refundId, boolean approved) {
        RefundRecord initial = refundRecordMapper.selectById(refundId);
        if (initial == null) {
            throw new BusinessException(404, "退款申请不存在");
        }

        // Use the same lock order as customer application: order, then refund.
        BookingOrder order = bookingOrderMapper.selectOne(
                new LambdaQueryWrapper<BookingOrder>()
                        .eq(BookingOrder::getOrderId, initial.getOrderId())
                        .last("FOR UPDATE")
        );
        if (order == null) {
            throw new BusinessException(404, "关联订单不存在");
        }

        RefundRecord refund = refundRecordMapper.selectByIdForUpdate(refundId);
        if (refund == null || !"PENDING".equals(refund.getRefundStatus())) {
            throw new BusinessException(409, "该退款申请已经审核，不能重复操作");
        }
        if (!"CONFIRMED".equals(order.getOrderStatus())
                || !("REFUNDING".equals(order.getPaymentStatus())
                     || "PAID".equals(order.getPaymentStatus()))) {
            throw new BusinessException(409, "当前订单状态不能审核退款");
        }

        if (approved) {
            completeMockRefund(refund, order);
        }

        String newStatus = approved ? "APPROVED" : "REJECTED";
        if (refundRecordMapper.updateStatus(refundId, newStatus) != 1) {
            throw new BusinessException(409, "该退款申请已经审核，不能重复操作");
        }

        if (approved) {
            order.setOrderStatus("REFUNDED");
            order.setPaymentStatus("REFUNDED");
        } else {
            order.setPaymentStatus("PAID");
        }
        if (bookingOrderMapper.updateById(order) != 1) {
            throw new BusinessException(500, "订单状态更新失败");
        }
    }

    private void completeMockRefund(RefundRecord refund, BookingOrder order) {
        List<Payment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, order.getOrderId())
                        .eq(Payment::getPaymentMethod, "MOCK")
                        .eq(Payment::getPaymentStatus, "SUCCESS")
        );
        BigDecimal paidAmount = payments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (refund.getRefundAmount() == null
                || refund.getRefundAmount().signum() <= 0
                || paidAmount.compareTo(refund.getRefundAmount()) < 0) {
            throw new BusinessException(409, "没有足额的模拟支付记录，无法退款");
        }

        List<RoomInventory> inventories = roomInventoryMapper.selectList(
                new LambdaQueryWrapper<RoomInventory>()
                        .eq(RoomInventory::getRoomTypeId, order.getRoomTypeId())
                        .ge(RoomInventory::getInventoryDate, order.getCheckInDate())
                        .lt(RoomInventory::getInventoryDate, order.getCheckOutDate())
                        .orderByAsc(RoomInventory::getInventoryDate)
                        .last("FOR UPDATE")
        );
        if (order.getNights() == null || inventories.size() != order.getNights()) {
            throw new BusinessException(409, "订单库存记录不完整，无法完成退款");
        }
        for (RoomInventory inventory : inventories) {
            if (inventory.getSoldQty() == null
                    || inventory.getSoldQty() < order.getRoomCount()) {
                throw new BusinessException(409, "已售库存数量不足，无法完成退款");
            }
            inventory.setSoldQty(inventory.getSoldQty() - order.getRoomCount());
            if (roomInventoryMapper.updateById(inventory) != 1) {
                throw new BusinessException(500, "库存更新失败");
            }
        }

        RefundPaymentRecord record = new RefundPaymentRecord();
        record.setRefundId(refund.getRefundId());
        record.setOrderId(order.getOrderId());
        record.setRefundNo(UUID.randomUUID().toString().replace("-", "").toUpperCase());
        record.setRefundAmount(refund.getRefundAmount());
        record.setRefundMethod("MOCK");
        record.setRefundStatus("SUCCESS");
        record.setInventorySyncStatus("PENDING");
        record.setRefundedTime(LocalDateTime.now());
        if (refundPaymentRecordMapper.insert(record) != 1) {
            throw new BusinessException(500, "退款流水保存失败");
        }
    }
}
