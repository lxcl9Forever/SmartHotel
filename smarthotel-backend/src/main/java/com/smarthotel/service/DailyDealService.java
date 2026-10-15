package com.smarthotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.booking.CreateBookingRequest;
import com.smarthotel.dto.promotion.DailyDealRequest;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.DailyDeal;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.entity.RoomPrice;
import com.smarthotel.entity.RoomType;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.DailyDealMapper;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.RoomPriceMapper;
import com.smarthotel.mapper.RoomTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyDealService {
    private final DailyDealMapper deals;
    private final BookingOrderMapper orders;
    private final RoomTypeMapper roomTypes;
    private final RoomPriceMapper prices;
    private final RoomInventoryMapper inventories;
    private final JdbcTemplate jdbc;

    public record Offer(Long id, Long roomTypeId, String typeCode, String roomTypeName,
                        LocalDate checkInDate, LocalDate checkOutDate,
                        LocalDateTime startTime, LocalDateTime endTime,
                        BigDecimal discountRate, BigDecimal originalPrice,
                        BigDecimal dealPrice, int remaining) {}

    public List<Offer> publicOffers() {
        List<DailyDeal> current = deals.selectList(new LambdaQueryWrapper<DailyDeal>()
                .eq(DailyDeal::getStatus, "ACTIVE")
                .ge(DailyDeal::getEndTime, LocalDateTime.now())
                .ge(DailyDeal::getCheckInDate, LocalDate.now())
                .orderByAsc(DailyDeal::getStartTime));
        List<Offer> result = new ArrayList<>();
        for (DailyDeal deal : current) {
            RoomType room = roomTypes.selectById(deal.getRoomTypeId());
            if (room == null || !Integer.valueOf(1).equals(room.getStatus())) continue;
            int nights = Math.toIntExact(ChronoUnit.DAYS.between(deal.getCheckInDate(), deal.getCheckOutDate()));
            if (nights <= 0) continue;
            List<RoomPrice> nightly = prices.selectList(new LambdaQueryWrapper<RoomPrice>()
                    .eq(RoomPrice::getRoomTypeId, deal.getRoomTypeId())
                    .ge(RoomPrice::getPriceDate, deal.getCheckInDate())
                    .lt(RoomPrice::getPriceDate, deal.getCheckOutDate()));
            List<RoomInventory> stock = inventories.selectList(new LambdaQueryWrapper<RoomInventory>()
                    .eq(RoomInventory::getRoomTypeId, deal.getRoomTypeId())
                    .ge(RoomInventory::getInventoryDate, deal.getCheckInDate())
                    .lt(RoomInventory::getInventoryDate, deal.getCheckOutDate()));
            if (nightly.size() != nights || stock.size() != nights) continue;
            BigDecimal original = nightly.stream().map(RoomPrice::getSalePrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int physical = stock.stream().mapToInt(item ->
                    item.getTotalQty() - item.getLockedQty() - item.getSoldQty()).min().orElse(0);
            int remaining = Math.max(0, Math.min(physical, deal.getQuota() - usedRooms(deal.getId())));
            result.add(new Offer(deal.getId(), deal.getRoomTypeId(), room.getTypeCode(),
                    room.getTypeName(), deal.getCheckInDate(), deal.getCheckOutDate(),
                    deal.getStartTime(), deal.getEndTime(), deal.getDiscountRate(),
                    original.setScale(2, RoundingMode.HALF_UP), discounted(original, deal.getDiscountRate()), remaining));
        }
        return result;
    }

    public List<DailyDeal> adminList() {
        return deals.selectList(new LambdaQueryWrapper<DailyDeal>()
                .ne(DailyDeal::getStatus, "DELETED").orderByDesc(DailyDeal::getId));
    }

    @Transactional
    public DailyDeal create(DailyDealRequest request) {
        validate(request);
        DailyDeal deal = copy(request);
        deal.setStatus("INACTIVE");
        deals.insert(deal);
        return deal;
    }

    @Transactional
    public DailyDeal update(Long id, DailyDealRequest request) {
        validate(request);
        DailyDeal old = requireLocked(id);
        if (hasAnyOrder(id)) throw new BusinessException(409, "已有订单使用该秒杀，不能修改规则；可关闭后新建活动");
        DailyDeal changed = copy(request);
        changed.setId(id);
        changed.setStatus(old.getStatus());
        deals.updateById(changed);
        return deals.selectById(id);
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        DailyDeal deal = requireLocked(id);
        if (enabled && !deal.getEndTime().isAfter(LocalDateTime.now()))
            throw new BusinessException(400, "活动已结束，不能开启");
        deal.setStatus(enabled ? "ACTIVE" : "INACTIVE");
        deals.updateById(deal);
    }

    @Transactional
    public void delete(Long id) {
        DailyDeal deal = requireLocked(id);
        deal.setStatus("DELETED");
        deals.updateById(deal);
    }

    /** The deal row serializes competing buyers. The order query is a locking/current read. */
    public DailyDeal lockForBooking(CreateBookingRequest request) {
        DailyDeal deal = requireLocked(request.getDailyDealId());
        LocalDateTime now = LocalDateTime.now();
        if (!"ACTIVE".equals(deal.getStatus()) || now.isBefore(deal.getStartTime())
                || !now.isBefore(deal.getEndTime()))
            throw new BusinessException(409, "秒杀活动尚未开始或已经结束");
        if (!deal.getRoomTypeId().equals(request.getRoomTypeId())
                || !deal.getCheckInDate().equals(request.getCheckInDate())
                || !deal.getCheckOutDate().equals(request.getCheckOutDate()))
            throw new BusinessException(400, "秒杀房型或入住日期不匹配");
        if (request.getCouponId() != null)
            throw new BusinessException(400, "秒杀订单不能使用优惠券");
        List<Integer> used = jdbc.queryForList(
                "SELECT room_count FROM booking_order WHERE daily_deal_id=? "
                        + "AND order_status NOT IN ('CANCELLED','CLOSED','REFUNDED') FOR UPDATE",
                Integer.class, deal.getId());
        int occupied = used.stream().mapToInt(Integer::intValue).sum();
        if (occupied + request.getRoomCount() > deal.getQuota())
            throw new BusinessException(409, "秒杀名额已抢完");
        return deal;
    }

    public PromotionService.Price price(DailyDeal deal, BigDecimal original) {
        BigDecimal amount = original.setScale(2, RoundingMode.HALF_UP);
        BigDecimal payable = discounted(amount, deal.getDiscountRate());
        if (payable.compareTo(new BigDecimal("0.01")) < 0)
            throw new BusinessException(400, "秒杀价格必须至少为0.01元");
        return new PromotionService.Price(null, amount, amount.subtract(payable), payable);
    }

    private static BigDecimal discounted(BigDecimal original, BigDecimal rate) {
        return original.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private int usedRooms(Long id) {
        return orders.selectList(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getDailyDealId, id)
                .notIn(BookingOrder::getOrderStatus, "CANCELLED", "CLOSED", "REFUNDED"))
                .stream().mapToInt(BookingOrder::getRoomCount).sum();
    }

    private boolean hasAnyOrder(Long id) {
        return orders.selectCount(new LambdaQueryWrapper<BookingOrder>()
                .eq(BookingOrder::getDailyDealId, id)) > 0;
    }

    private DailyDeal requireLocked(Long id) {
        DailyDeal deal = id == null ? null : deals.selectOne(new LambdaQueryWrapper<DailyDeal>()
                .eq(DailyDeal::getId, id).last("FOR UPDATE"));
        if (deal == null || "DELETED".equals(deal.getStatus()))
            throw new BusinessException(404, "秒杀活动不存在");
        return deal;
    }

    private DailyDeal copy(DailyDealRequest request) {
        DailyDeal deal = new DailyDeal();
        deal.setRoomTypeId(request.getRoomTypeId());
        deal.setCheckInDate(request.getCheckInDate());
        deal.setCheckOutDate(request.getCheckOutDate());
        deal.setStartTime(request.getStartTime());
        deal.setEndTime(request.getEndTime());
        deal.setQuota(request.getQuota());
        deal.setDiscountRate(request.getDiscountRate());
        return deal;
    }

    private void validate(DailyDealRequest request) {
        if (request.getCheckInDate().isBefore(LocalDate.now())
                || !request.getCheckInDate().isBefore(request.getCheckOutDate()))
            throw new BusinessException(400, "请填写有效的入住和退房日期");
        if (!request.getStartTime().isBefore(request.getEndTime())
                || !request.getEndTime().isAfter(LocalDateTime.now()))
            throw new BusinessException(400, "请填写有效的秒杀开始和结束时间");
        if (request.getDiscountRate().compareTo(BigDecimal.ZERO) <= 0
                || request.getDiscountRate().compareTo(BigDecimal.ONE) >= 0
                || request.getDiscountRate().scale() > 4)
            throw new BusinessException(400, "折扣率须大于0且小于1，最多4位小数");
        RoomType room = roomTypes.selectById(request.getRoomTypeId());
        if (room == null || !Integer.valueOf(1).equals(room.getStatus()))
            throw new BusinessException(400, "房型不存在或已停售");
        int nights = Math.toIntExact(ChronoUnit.DAYS.between(
                request.getCheckInDate(), request.getCheckOutDate()));
        List<RoomPrice> nightly = prices.selectList(new LambdaQueryWrapper<RoomPrice>()
                .eq(RoomPrice::getRoomTypeId, request.getRoomTypeId())
                .ge(RoomPrice::getPriceDate, request.getCheckInDate())
                .lt(RoomPrice::getPriceDate, request.getCheckOutDate()));
        List<RoomInventory> stock = inventories.selectList(new LambdaQueryWrapper<RoomInventory>()
                .eq(RoomInventory::getRoomTypeId, request.getRoomTypeId())
                .ge(RoomInventory::getInventoryDate, request.getCheckInDate())
                .lt(RoomInventory::getInventoryDate, request.getCheckOutDate()));
        if (nightly.size() != nights || stock.size() != nights)
            throw new BusinessException(400, "入住期间房价或库存数据不完整");
        if (stock.stream().mapToInt(RoomInventory::getTotalQty).min().orElse(0) < request.getQuota())
            throw new BusinessException(400, "秒杀间数不能超过房型库存总量");
        BigDecimal total = nightly.stream().map(RoomPrice::getSalePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (discounted(total, request.getDiscountRate()).compareTo(new BigDecimal("0.01")) < 0)
            throw new BusinessException(400, "秒杀价必须至少为0.01元");
    }
}
