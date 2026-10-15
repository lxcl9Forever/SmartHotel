package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.stay.ExtendStayRequest;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.Customer;
import com.smarthotel.entity.Payment;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.entity.RoomPrice;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.CustomerMapper;
import com.smarthotel.mapper.PaymentMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.RoomPriceMapper;
import com.smarthotel.service.InventoryRedisService;
import com.smarthotel.service.StayExtensionService;
import com.smarthotel.vo.stay.ExtendStayPreviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StayExtensionServiceImpl implements StayExtensionService {
    private static final int MAX_EXTRA_NIGHTS = 30;

    private final BookingOrderMapper bookingOrderMapper;
    private final CustomerMapper customerMapper;
    private final RoomPriceMapper roomPriceMapper;
    private final RoomInventoryMapper roomInventoryMapper;
    private final InventoryRedisService inventoryRedisService;
    private final PaymentMapper paymentMapper;

    @Override
    public ExtendStayPreviewVO preview(ExtendStayRequest request) {
        BookingOrder order = loadOrder(request, false, null);
        return calculate(order, request, false).preview();
    }

    @Override
    public ExtendStayPreviewVO previewForCustomer(Long userId, ExtendStayRequest request) {
        BookingOrder order = loadOrder(request, false, customerId(userId));
        return calculate(order, request, false).preview();
    }

    @Override
    @Transactional
    public ExtendStayPreviewVO extend(ExtendStayRequest request) {
        return extendLocked(request, null);
    }

    @Override
    @Transactional
    public ExtendStayPreviewVO extendForCustomer(Long userId, ExtendStayRequest request) {
        return extendLocked(request, customerId(userId));
    }

    private ExtendStayPreviewVO extendLocked(ExtendStayRequest request, Long ownedCustomerId) {
        BookingOrder order = loadOrder(request, true, ownedCustomerId);
        Calculation calculation = calculate(order, request, true);
        ExtendStayPreviewVO preview = calculation.preview();
        if (!Boolean.TRUE.equals(preview.getAvailable())) {
            throw new BusinessException(409, "新增日期库存不足");
        }

        LocalDate oldCheckout = order.getCheckOutDate();
        LocalDate newCheckout = request.getNewCheckOutDate();
        int roomCount = order.getRoomCount();
        Long locked = inventoryRedisService.lockInventory(
                order.getRoomTypeId(), oldCheckout, newCheckout, roomCount);
        if (!Long.valueOf(1).equals(locked)) {
            throw new BusinessException(409, "新增日期 Redis 库存不足或尚未初始化");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Long released = inventoryRedisService.releaseInventory(
                                order.getRoomTypeId(), oldCheckout, newCheckout, roomCount);
                        if (!Long.valueOf(1).equals(released)) {
                            log.error("Redis compensation failed for stay extension order {}: {}",
                                    order.getOrderId(), released);
                        }
                    } catch (RuntimeException error) {
                        log.error("Redis compensation failed for stay extension order {}",
                                order.getOrderId(), error);
                    }
                }
            }
        });

        for (RoomInventory day : calculation.inventories()) {
            day.setSoldQty(day.getSoldQty() + roomCount);
            if (roomInventoryMapper.updateById(day) != 1) {
                throw new IllegalStateException("续住库存更新失败: " + day.getInventoryDate());
            }
        }
        BigDecimal oldTotal = order.getTotalAmount();
        order.setCheckOutDate(newCheckout);
        order.setNights(order.getNights() + preview.getExtraNights());
        order.setTotalAmount(oldTotal.add(preview.getExtraAmount()));
        order.setOriginalAmount((order.getOriginalAmount() == null
                ? oldTotal : order.getOriginalAmount()).add(preview.getExtraAmount()));
        if (bookingOrderMapper.updateById(order) != 1) {
            throw new IllegalStateException("续住订单更新失败: " + order.getOrderId());
        }

        // The existing checkout is simulated, so the extra charge follows the same MOCK payment mode.
        Payment payment = new Payment();
        payment.setPaymentNo(UUID.randomUUID().toString().replace("-", "").toUpperCase());
        payment.setOrderId(order.getOrderId());
        payment.setPaymentMethod("MOCK");
        payment.setAmount(preview.getExtraAmount());
        payment.setPaymentStatus("SUCCESS");
        payment.setPaidTime(LocalDateTime.now());
        if (paymentMapper.insert(payment) != 1) {
            throw new IllegalStateException("续住支付流水保存失败: " + order.getOrderId());
        }
        preview.setSuccess(true);
        return preview;
    }

    private BookingOrder loadOrder(ExtendStayRequest request, boolean lock, Long ownedCustomerId) {
        if (request == null || request.getOrderId() == null) {
            throw new BusinessException(400, "请选择订单");
        }
        LambdaQueryWrapper<BookingOrder> query = new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getOrderId, request.getOrderId());
        if (lock) query.last("FOR UPDATE");
        BookingOrder order = bookingOrderMapper.selectOne(query);
        if (order == null || (ownedCustomerId != null
                && !ownedCustomerId.equals(order.getCustomerId()))) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!"CHECKED_IN".equals(order.getOrderStatus())
                || !"PAID".equals(order.getPaymentStatus())) {
            throw new BusinessException(409, "只有已支付且入住中的订单可以续住");
        }
        return order;
    }

    private Long customerId(Long userId) {
        if (userId == null) throw new BusinessException(401, "请先登录");
        Customer customer = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getUserId, userId));
        if (customer == null) throw new BusinessException(403, "当前账号没有客户档案");
        return customer.getCustomerId();
    }

    private Calculation calculate(BookingOrder order, ExtendStayRequest request, boolean lockInventory) {
        LocalDate oldCheckout = order.getCheckOutDate();
        LocalDate newCheckout = request.getNewCheckOutDate();
        if (newCheckout == null || !newCheckout.isAfter(oldCheckout)) {
            throw new BusinessException(400, "新的退房日期必须晚于当前退房日期");
        }
        int extraNights = Math.toIntExact(ChronoUnit.DAYS.between(oldCheckout, newCheckout));
        if (extraNights > MAX_EXTRA_NIGHTS) {
            throw new BusinessException(400, "每次最多续住 30 晚");
        }
        List<RoomPrice> prices = roomPriceMapper.selectList(new LambdaQueryWrapper<RoomPrice>()
                .eq(RoomPrice::getRoomTypeId, order.getRoomTypeId())
                .ge(RoomPrice::getPriceDate, oldCheckout)
                .lt(RoomPrice::getPriceDate, newCheckout)
                .orderByAsc(RoomPrice::getPriceDate));
        if (prices.size() != extraNights) {
            throw new BusinessException(409, "新增日期没有完整房价");
        }
        BigDecimal extraAmount = prices.stream().map(RoomPrice::getSalePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(order.getRoomCount()));

        LambdaQueryWrapper<RoomInventory> inventoryQuery = new LambdaQueryWrapper<RoomInventory>()
                .eq(RoomInventory::getRoomTypeId, order.getRoomTypeId())
                .ge(RoomInventory::getInventoryDate, oldCheckout)
                .lt(RoomInventory::getInventoryDate, newCheckout)
                .orderByAsc(RoomInventory::getInventoryDate);
        if (lockInventory) inventoryQuery.last("FOR UPDATE");
        List<RoomInventory> inventories = roomInventoryMapper.selectList(inventoryQuery);
        if (inventories.size() != extraNights) {
            throw new BusinessException(409, "新增日期没有完整库存");
        }
        int available = inventories.stream().mapToInt(day -> day.getTotalQty()
                - day.getLockedQty() - day.getSoldQty()).min().orElse(0);

        ExtendStayPreviewVO preview = new ExtendStayPreviewVO();
        preview.setOrderId(order.getOrderId());
        preview.setOldCheckOutDate(oldCheckout);
        preview.setNewCheckOutDate(newCheckout);
        preview.setExtraNights(extraNights);
        preview.setExtraAmount(extraAmount);
        preview.setAvailableRoom(available);
        preview.setAvailable(available >= order.getRoomCount());
        return new Calculation(preview, inventories);
    }

    private record Calculation(ExtendStayPreviewVO preview, List<RoomInventory> inventories) {
    }
}
