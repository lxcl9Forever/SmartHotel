package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.booking.CreateBookingRequest;
import com.smarthotel.entity.*;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.*;
import com.smarthotel.service.BookingService;
import com.smarthotel.service.InventoryRedisService;
import com.smarthotel.service.OrderTimeoutProducer;
import com.smarthotel.service.PromotionService;
import com.smarthotel.service.DailyCheckinService;
import com.smarthotel.service.DailyDealService;
import com.smarthotel.vo.BookingOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.smarthotel.dto.booking.BookingGuestRequest;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import com.smarthotel.mapper.PaymentMapper;
import com.smarthotel.vo.BookingGuestVO;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final SysUserMapper sysUserMapper;

    private final CustomerMapper customerMapper;

    private final RoomTypeMapper roomTypeMapper;

    private final RoomInventoryMapper roomInventoryMapper;

    private final RoomPriceMapper roomPriceMapper;

    private final BookingOrderMapper bookingOrderMapper;

    private final BookingGuestMapper bookingGuestMapper;

    private final PaymentMapper paymentMapper;
    private final RefundRecordMapper refundRecordMapper;

    private final InventoryRedisService inventoryRedisService;
    private final OrderTimeoutProducer orderTimeoutProducer;
    private final PromotionService promotionService;
    private final DailyCheckinService dailyCheckinService;
    private final DailyDealService dailyDealService;

    @Value("${smarthotel.order.payment-timeout-minutes:30}")
    private int paymentTimeoutMinutes;

    @Override
    @Transactional
    public BookingOrderVO createBooking(
            String username,
            CreateBookingRequest request) {

        /*
         * 1. 找当前登录账号
         */
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException(401, "当前登录用户不存在");
        }


        /*
         * 2. 找客户档案
         */
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getUserId, user.getUserId())
        );

        if (customer == null) {
            throw new BusinessException(403, "当前账号没有客户档案");
        }


        /*
         * 3. 校验日期
         */
        if (!request.getCheckInDate()
                .isBefore(request.getCheckOutDate())) {

            throw new BusinessException(
                    400,
                    "退房日期必须晚于入住日期"
            );
        }

        long nights = ChronoUnit.DAYS.between(
                request.getCheckInDate(),
                request.getCheckOutDate()
        );


        /*
         * 4. 查询房型
         */
        RoomType roomType =
                roomTypeMapper.selectById(
                        request.getRoomTypeId()
                );

        if (roomType == null
                || !Integer.valueOf(1)
                .equals(roomType.getStatus())) {

            throw new BusinessException(
                    404,
                    "房型不存在或已停售"
            );
        }


        /*
         * 5. 校验入住人数
         */
        if (roomType.getMaxGuests()
                * request.getRoomCount()
                < request.getGuestCount()) {

            throw new BusinessException(
                    400,
                    "入住人数超过所选房间容量"
            );
        }

        /*
         * 入住人数必须和入住人信息数量一致
         */
        if (request.getGuests() == null
                || request.getGuests().size()
                != request.getGuestCount()) {

            throw new BusinessException(
                    400,
                    "入住人信息数量必须与入住人数一致"
            );
        }

        /*
         * 6. 查询并锁住入住期间库存行
         *
         * FOR UPDATE 非常关键：
         * 当前事务结束前，其他下单事务不能同时修改这些库存行。
         */
        /*
         * 6.1 Redis Lua原子锁库存
         *
         * 成功:
         * 1
         *
         * 失败:
         * -1 库存不足
         * -2 Redis库存不存在
         */
        DailyDeal dailyDeal = request.getDailyDealId() == null
                ? null : dailyDealService.lockForBooking(request);

        Long lockResult =
                inventoryRedisService.lockInventory(
                        request.getRoomTypeId(),
                        request.getCheckInDate(),
                        request.getCheckOutDate(),
                        request.getRoomCount()
                );


        if (lockResult == null
                || lockResult != 1) {

            throw new BusinessException(
                    409,
                    "Redis库存不足"
            );
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Long result = inventoryRedisService.releaseInventory(
                                request.getRoomTypeId(), request.getCheckInDate(),
                                request.getCheckOutDate(), request.getRoomCount());
                        if (result == null || result != 1L) {
                            log.warn("Redis inventory rollback compensation returned {}", result);
                        }
                    } catch (RuntimeException error) {
                        log.error("Redis inventory rollback compensation failed; resync inventory", error);
                    }
                }
            }
        });
        List<RoomInventory> inventories =
                roomInventoryMapper.selectList(
                        new LambdaQueryWrapper<RoomInventory>()
                                .eq(
                                        RoomInventory::getRoomTypeId,
                                        request.getRoomTypeId()
                                )
                                .ge(
                                        RoomInventory::getInventoryDate,
                                        request.getCheckInDate()
                                )
                                .lt(
                                        RoomInventory::getInventoryDate,
                                        request.getCheckOutDate()
                                )
                                .orderByAsc(
                                        RoomInventory::getInventoryDate
                                )
                                .last("FOR UPDATE")
                );

        if (inventories.size() != nights) {
            throw new BusinessException(
                    400,
                    "所选日期暂无完整房量数据"
            );
        }


        /*
         * 7. 再次检查每天库存
         */
        for (RoomInventory inventory : inventories) {

            int availableQty =
                    inventory.getTotalQty()
                            - inventory.getLockedQty()
                            - inventory.getSoldQty();

            if (availableQty < request.getRoomCount()) {
                throw new BusinessException(
                        409,
                        "所选房型库存不足"
                );
            }
        }


        /*
         * 8. 查询每天价格
         */
        List<RoomPrice> prices =
                roomPriceMapper.selectList(
                        new LambdaQueryWrapper<RoomPrice>()
                                .eq(
                                        RoomPrice::getRoomTypeId,
                                        request.getRoomTypeId()
                                )
                                .ge(
                                        RoomPrice::getPriceDate,
                                        request.getCheckInDate()
                                )
                                .lt(
                                        RoomPrice::getPriceDate,
                                        request.getCheckOutDate()
                                )
                                .orderByAsc(
                                        RoomPrice::getPriceDate
                                )
                );

        if (prices.size() != nights) {
            throw new BusinessException(
                    400,
                    "所选日期暂无完整房价数据"
            );
        }


        /*
         * 9. 计算订单价格
         */
        BigDecimal pricePerRoom =
                prices.stream()
                        .map(RoomPrice::getSalePrice)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        BigDecimal totalAmount =
                pricePerRoom.multiply(
                        BigDecimal.valueOf(
                                request.getRoomCount()
                        )
                );
        PromotionService.Price promotionPrice = dailyDeal == null
                ? promotionService.calculate(totalAmount)
                : dailyDealService.price(dailyDeal, totalAmount);
        BigDecimal couponDiscount = dailyDeal == null
                ? dailyCheckinService.lockDiscount(
                    customer.getCustomerId(), request.getCouponId(), promotionPrice.payableAmount())
                : BigDecimal.ZERO;


        /*
         * 10. 锁定每天库存
         */
        for (RoomInventory inventory : inventories) {

            inventory.setLockedQty(
                    inventory.getLockedQty()
                            + request.getRoomCount()
            );

            roomInventoryMapper.updateById(inventory);
        }


        /*
         * 11. 创建订单
         */
        BookingOrder order = new BookingOrder();

        order.setOrderNo(
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase()
        );

        order.setCustomerId(
                customer.getCustomerId()
        );

        order.setRoomTypeId(
                request.getRoomTypeId()
        );

        order.setRoomCount(
                request.getRoomCount()
        );

        order.setGuestCount(
                request.getGuestCount()
        );

        order.setCheckInDate(
                request.getCheckInDate()
        );

        order.setCheckOutDate(
                request.getCheckOutDate()
        );

        order.setNights(
                Math.toIntExact(nights)
        );

        order.setOriginalAmount(promotionPrice.originalAmount());
        order.setDiscountAmount(promotionPrice.discountAmount().add(couponDiscount));
        order.setPromotionId(promotionPrice.promotionId());
        order.setDailyDealId(request.getDailyDealId());
        order.setCouponId(request.getCouponId());
        order.setCouponDiscountAmount(couponDiscount);
        order.setTotalAmount(promotionPrice.payableAmount().subtract(couponDiscount));

        order.setContactName(
                request.getContactName()
        );

        order.setContactPhone(
                request.getContactPhone()
        );

        order.setOrderStatus("WAIT_PAY");

        order.setPaymentStatus("UNPAID");

        order.setExpireTime(
                LocalDateTime.now()
                        .plusMinutes(paymentTimeoutMinutes)
        );

        order.setRemark(
                request.getRemark()
        );
        //保存订单
        bookingOrderMapper.insert(order);
        if (request.getCouponId() != null) {
            dailyCheckinService.reserve(customer.getCustomerId(), request.getCouponId(), order.getOrderId());
        }
        orderTimeoutProducer.sendAfterCommit(order.getOrderId(), order.getExpireTime());

        /*
         * 保存入住人
         */
        for (int i = 0;
             i < request.getGuests().size();
             i++) {

            BookingGuestRequest guestRequest =
                    request.getGuests().get(i);


            BookingGuest guest =
                    new BookingGuest();


            guest.setOrderId(
                    order.getOrderId()
            );


            guest.setGuestName(
                    guestRequest.getGuestName()
            );


            guest.setPhone(
                    guestRequest.getPhone()
            );


            String idType =
                    guestRequest.getIdType();

            if (idType == null
                    || idType.isBlank()) {

                idType = "ID_CARD";
            }

            guest.setIdType(idType);


            guest.setIdNumber(
                    guestRequest.getIdNumber()
            );


            /*
             * 第一个入住人作为主要入住人
             */
            guest.setIsPrimary(
                    i == 0 ? 1 : 0
            );


            guest.setCreateTime(
                    LocalDateTime.now()
            );


            bookingGuestMapper.insert(
                    guest
            );
        }

        /*
         * 12. 返回订单信息
         */

        //返回订单
        if (order.getTotalAmount().signum() == 0) {
            // A no-minimum coupon may cover the whole booking; no external payment is needed.
            return mockPaySuccess(username, order.getOrderId());
        }
        return toBookingOrderVO(order);
    }

    /**
     * 查询当前客户的所有订单
     */
    @Override
    public List<BookingOrderVO> listMyBookings(String username) {

        /*
         * 1. 根据用户名找到当前登录账号
         */
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException(
                    401,
                    "当前登录用户不存在"
            );
        }


        /*
         * 2. 找到客户档案
         */
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getUserId, user.getUserId())
        );

        if (customer == null) {
            throw new BusinessException(
                    403,
                    "当前账号没有客户档案"
            );
        }


        /*
         * 3. 只查询这个客户自己的订单
         *
         * 按创建时间倒序：
         * 最新订单放最前面
         */
        List<BookingOrder> orders =
                bookingOrderMapper.selectList(
                        new LambdaQueryWrapper<BookingOrder>()
                                .eq(
                                        BookingOrder::getCustomerId,
                                        customer.getCustomerId()
                                )
                                .orderByDesc(
                                        BookingOrder::getCreateTime
                                )
                );


        /*
         * 4. Entity 转 VO
         */
        List<BookingOrderVO> result = new ArrayList<>();

        for (BookingOrder order : orders) {
            result.add(toBookingOrderVO(order));
        }

        return result;
    }


    /**
     * 查询当前客户自己的某一个订单
     */
    @Override
    public BookingOrderVO getMyBooking(
            String username,
            Long orderId) {

        /*
         * 1. 找当前用户
         */
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException(
                    401,
                    "当前登录用户不存在"
            );
        }


        /*
         * 2. 找客户档案
         */
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getUserId, user.getUserId())
        );

        if (customer == null) {
            throw new BusinessException(
                    403,
                    "当前账号没有客户档案"
            );
        }


        /*
         * 3. 查询订单
         *
         * 注意：
         * 这里不仅根据 orderId 查，
         * 还必须根据 customerId 查。
         *
         * 防止用户查看别人的订单。
         */
        BookingOrder order =
                bookingOrderMapper.selectOne(
                        new LambdaQueryWrapper<BookingOrder>()
                                .eq(
                                        BookingOrder::getOrderId,
                                        orderId
                                )
                                .eq(
                                        BookingOrder::getCustomerId,
                                        customer.getCustomerId()
                                )
                );

        if (order == null) {
            throw new BusinessException(
                    404,
                    "订单不存在"
            );
        }

        BookingOrderVO vo =
                toBookingOrderVO(order);

        RefundRecord refundRecord = refundRecordMapper.selectByOrderId(orderId);
        if (refundRecord != null) {
            vo.setRefundStatus(refundRecord.getRefundStatus());
        }


        /*
         * 查询当前订单全部入住人
         */
        List<BookingGuest> guests =
                bookingGuestMapper.selectList(
                        new LambdaQueryWrapper<BookingGuest>()
                                .eq(
                                        BookingGuest::getOrderId,
                                        order.getOrderId()
                                )
                                .orderByDesc(
                                        BookingGuest::getIsPrimary
                                )
                                .orderByAsc(
                                        BookingGuest::getGuestId
                                )
                );


        List<BookingGuestVO> guestVOList =
                new ArrayList<>();


        for (BookingGuest guest : guests) {

            BookingGuestVO guestVO =
                    new BookingGuestVO();

            guestVO.setGuestId(
                    guest.getGuestId()
            );

            guestVO.setGuestName(
                    guest.getGuestName()
            );

            guestVO.setPhone(
                    guest.getPhone()
            );

            guestVO.setIdType(
                    guest.getIdType()
            );

            guestVO.setIdNumber(
                    guest.getIdNumber()
            );

            guestVO.setIsPrimary(
                    guest.getIsPrimary()
            );

            guestVOList.add(
                    guestVO
            );
        }


        vo.setGuests(
                guestVOList
        );


        return vo;
    }


    /**
     * BookingOrder Entity 转 BookingOrderVO
     */
    private BookingOrderVO toBookingOrderVO(
            BookingOrder order) {

        RoomType roomType =
                roomTypeMapper.selectById(
                        order.getRoomTypeId()
                );

        BookingOrderVO vo =
                new BookingOrderVO();

        vo.setOrderId(
                order.getOrderId()
        );

        vo.setOrderNo(
                order.getOrderNo()
        );

        vo.setRoomTypeId(
                order.getRoomTypeId()
        );

        if (roomType != null) {
            vo.setRoomTypeName(
                    roomType.getTypeName()
            );
        }

        vo.setRoomCount(
                order.getRoomCount()
        );

        vo.setGuestCount(
                order.getGuestCount()
        );

        vo.setCheckInDate(
                order.getCheckInDate()
        );

        vo.setCheckOutDate(
                order.getCheckOutDate()
        );

        vo.setNights(
                order.getNights()
        );

        vo.setTotalAmount(
                order.getTotalAmount()
        );
        vo.setOriginalAmount(order.getOriginalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPromotionId(order.getPromotionId());
        vo.setDailyDealId(order.getDailyDealId());
        vo.setCouponId(order.getCouponId());
        vo.setCouponDiscountAmount(order.getCouponDiscountAmount());

        vo.setContactName(
                order.getContactName()
        );

        vo.setContactPhone(
                order.getContactPhone()
        );

        vo.setOrderStatus(
                order.getOrderStatus()
        );

        vo.setPaymentStatus(
                order.getPaymentStatus()
        );

        vo.setExpireTime(
                order.getExpireTime()
        );

        vo.setRemark(
                order.getRemark()
        );

        vo.setCreateTime(
                order.getCreateTime()
        );

        return vo;
    }


    /**
     * 客户主动取消待支付订单
     */
    @Override
    @Transactional
    public void cancelMyBooking(
            String username,
            Long orderId) {

        /*
         * 1. 找当前登录用户
         */
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException(
                    401,
                    "当前登录用户不存在"
            );
        }


        /*
         * 2. 找客户档案
         */
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(Customer::getUserId, user.getUserId())
        );

        if (customer == null) {
            throw new BusinessException(
                    403,
                    "当前账号没有客户档案"
            );
        }


        /*
         * 3. 查询并锁住订单
         *
         * 必须同时检查 customerId，
         * 防止用户取消别人的订单。
         */
        BookingOrder order =
                bookingOrderMapper.selectOne(
                        new LambdaQueryWrapper<BookingOrder>()
                                .eq(
                                        BookingOrder::getOrderId,
                                        orderId
                                )
                                .eq(
                                        BookingOrder::getCustomerId,
                                        customer.getCustomerId()
                                )
                                .last("FOR UPDATE")
                );

        if (order == null) {
            throw new BusinessException(
                    404,
                    "订单不存在"
            );
        }


        /*
         * 当前阶段只允许取消 WAIT_PAY 订单
         */
        if (!"WAIT_PAY".equals(order.getOrderStatus())) {
            throw new BusinessException(
                    400,
                    "当前订单状态不能取消"
            );
        }


        /*
         * 4. 锁住该订单占用的每日库存
         */
        List<RoomInventory> inventories =
                roomInventoryMapper.selectList(
                        new LambdaQueryWrapper<RoomInventory>()
                                .eq(
                                        RoomInventory::getRoomTypeId,
                                        order.getRoomTypeId()
                                )
                                .ge(
                                        RoomInventory::getInventoryDate,
                                        order.getCheckInDate()
                                )
                                .lt(
                                        RoomInventory::getInventoryDate,
                                        order.getCheckOutDate()
                                )
                                .orderByAsc(
                                        RoomInventory::getInventoryDate
                                )
                                .last("FOR UPDATE")
                );


        /*
         * 5. 释放 locked_qty
         */
        // Redis恢复库存
        inventoryRedisService.releaseInventory(
                order.getRoomTypeId(),
                order.getCheckInDate(),
                order.getCheckOutDate(),
                order.getRoomCount()
        );


        // MySQL恢复库存
        for (RoomInventory inventory : inventories) {

            int newLockedQty =
                    inventory.getLockedQty()
                            - order.getRoomCount();

            /*
             * 理论上不应该小于0。
             * 防止测试数据异常导致负库存。
             */
            if (newLockedQty < 0) {
                throw new BusinessException(
                        500,
                        "库存锁定数据异常"
                );
            }

            inventory.setLockedQty(newLockedQty);

            roomInventoryMapper.updateById(
                    inventory
            );
        }


        /*
         * 6. 修改订单状态
         */
        order.setOrderStatus("CANCELLED");

        bookingOrderMapper.updateById(order);
        dailyCheckinService.release(order.getOrderId());
    }

    /**
     * 自动关闭已经超过支付时间的订单
     */
    @Override
    @Transactional
    public void closeExpiredBookings() {

        /*
         * 1. 找出已经超时但仍处于 WAIT_PAY 的订单
         */
        List<BookingOrder> expiredOrders =
                bookingOrderMapper.selectList(
                        new LambdaQueryWrapper<BookingOrder>()
                                .eq(
                                        BookingOrder::getOrderStatus,
                                        "WAIT_PAY"
                                )
                                .le(
                                        BookingOrder::getExpireTime,
                                        LocalDateTime.now()
                                )
                );


        /*
         * 2. 一个一个处理
         */
        for (BookingOrder order : expiredOrders) {

            /*
             * 再锁一次订单。
             *
             * 这样以后接支付回调时，
             * 可以降低“支付”和“超时关闭”同时发生的问题。
             */
            BookingOrder lockedOrder =
                    bookingOrderMapper.selectOne(
                            new LambdaQueryWrapper<BookingOrder>()
                                    .eq(
                                            BookingOrder::getOrderId,
                                            order.getOrderId()
                                    )
                                    .last("FOR UPDATE")
                    );


            /*
             * 订单可能已经被其他逻辑处理
             */
            if (lockedOrder == null) {
                continue;
            }

            if (!"WAIT_PAY".equals(
                    lockedOrder.getOrderStatus())) {
                continue;
            }

            /*
             * 再判断一次是否真的超时
             */
            if (lockedOrder.getExpireTime()
                    .isAfter(LocalDateTime.now())) {

                continue;
            }


            /*
             * 3. 查询并锁定库存
             */
            List<RoomInventory> inventories =
                    roomInventoryMapper.selectList(
                            new LambdaQueryWrapper<RoomInventory>()
                                    .eq(
                                            RoomInventory::getRoomTypeId,
                                            lockedOrder.getRoomTypeId()
                                    )
                                    .ge(
                                            RoomInventory::getInventoryDate,
                                            lockedOrder.getCheckInDate()
                                    )
                                    .lt(
                                            RoomInventory::getInventoryDate,
                                            lockedOrder.getCheckOutDate()
                                    )
                                    .orderByAsc(
                                            RoomInventory::getInventoryDate
                                    )
                                    .last("FOR UPDATE")
                    );


            /*
             * 4. 释放库存
             */
            inventoryRedisService.releaseInventory(
                    lockedOrder.getRoomTypeId(),
                    lockedOrder.getCheckInDate(),
                    lockedOrder.getCheckOutDate(),
                    lockedOrder.getRoomCount()
            );
            for (RoomInventory inventory : inventories) {

                int newLockedQty =
                        inventory.getLockedQty()
                                - lockedOrder.getRoomCount();

                if (newLockedQty < 0) {
                    throw new BusinessException(
                            500,
                            "库存锁定数据异常"
                    );
                }

                inventory.setLockedQty(
                        newLockedQty
                );

                roomInventoryMapper.updateById(
                        inventory
                );
            }


            /*
             * 5. WAIT_PAY → CLOSED
             *
             * CLOSED 表示：
             * 因支付超时自动关闭。
             *
             * CANCELLED 则表示：
             * 用户主动取消。
             */
            lockedOrder.setOrderStatus("CLOSED");

            bookingOrderMapper.updateById(
                    lockedOrder
            );
            dailyCheckinService.release(lockedOrder.getOrderId());
        }
    }

    /**
     * 开发阶段模拟支付成功
     *
     * 后续接支付宝时，
     * 支付宝异步回调最终也会执行类似的核心逻辑。
     */
    @Override
    @Transactional
    public BookingOrderVO mockPaySuccess(
            String username,
            Long orderId) {

        /*
         * 1. 当前登录账号
         */
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );

        if (user == null) {
            throw new BusinessException(
                    401,
                    "当前登录用户不存在"
            );
        }


        /*
         * 2. 当前客户
         */
        Customer customer = customerMapper.selectOne(
                new LambdaQueryWrapper<Customer>()
                        .eq(
                                Customer::getUserId,
                                user.getUserId()
                        )
        );

        if (customer == null) {
            throw new BusinessException(
                    403,
                    "当前账号没有客户档案"
            );
        }


        /*
         * 3. 查询并锁住订单
         *
         * 同时用 customerId 限制：
         * 客户只能支付自己的订单。
         */
        BookingOrder order =
                bookingOrderMapper.selectOne(
                        new LambdaQueryWrapper<BookingOrder>()
                                .eq(
                                        BookingOrder::getOrderId,
                                        orderId
                                )
                                .eq(
                                        BookingOrder::getCustomerId,
                                        customer.getCustomerId()
                                )
                                .last("FOR UPDATE")
                );

        if (order == null) {
            throw new BusinessException(
                    404,
                    "订单不存在"
            );
        }


        /*
         * 4. 幂等判断
         *
         * 如果已经支付过，
         * 不允许再次重复支付。
         */
        if ("PAID".equals(
                order.getPaymentStatus())) {

            throw new BusinessException(
                    400,
                    "订单已经支付，请勿重复支付"
            );
        }


        /*
         * 5. 只有 WAIT_PAY 才能支付
         */
        if (!"WAIT_PAY".equals(
                order.getOrderStatus())) {

            throw new BusinessException(
                    400,
                    "当前订单状态不能支付"
            );
        }


        /*
         * 6. 判断是否已经过期
         */
        if (order.getExpireTime() != null
                && order.getExpireTime()
                .isBefore(LocalDateTime.now())) {

            throw new BusinessException(
                    400,
                    "订单已超过支付时间"
            );
        }


        /*
         * 7. 查询并锁住这个订单占用的所有库存行
         */
        List<RoomInventory> inventories =
                roomInventoryMapper.selectList(
                        new LambdaQueryWrapper<RoomInventory>()
                                .eq(
                                        RoomInventory::getRoomTypeId,
                                        order.getRoomTypeId()
                                )
                                .ge(
                                        RoomInventory::getInventoryDate,
                                        order.getCheckInDate()
                                )
                                .lt(
                                        RoomInventory::getInventoryDate,
                                        order.getCheckOutDate()
                                )
                                .orderByAsc(
                                        RoomInventory::getInventoryDate
                                )
                                .last("FOR UPDATE")
                );


        /*
         * 必须覆盖整个入住期间。
         */
        if (inventories.size()
                != order.getNights()) {

            throw new BusinessException(
                    500,
                    "订单库存数据异常"
            );
        }


        /*
         * 8. locked → sold
         */
        for (RoomInventory inventory : inventories) {

            if (inventory.getLockedQty()
                    < order.getRoomCount()) {

                throw new BusinessException(
                        500,
                        "库存锁定数量异常"
                );
            }

            inventory.setLockedQty(
                    inventory.getLockedQty()
                            - order.getRoomCount()
            );

            inventory.setSoldQty(
                    inventory.getSoldQty()
                            + order.getRoomCount()
            );

            roomInventoryMapper.updateById(
                    inventory
            );
        }


        /*
         * 9. 创建支付记录
         */
        Payment payment = new Payment();

        payment.setPaymentNo(
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase()
        );

        payment.setOrderId(
                order.getOrderId()
        );

        /*
         * 当前是模拟支付。
         *
         * 将来支付宝：
         * paymentMethod = ALIPAY
         */
        payment.setPaymentMethod(order.getTotalAmount().signum() == 0 ? "COUPON" : "MOCK");

        payment.setAmount(
                order.getTotalAmount()
        );

        payment.setPaymentStatus(
                "SUCCESS"
        );

        payment.setPaidTime(
                LocalDateTime.now()
        );

        paymentMapper.insert(payment);


        /*
         * 10. 修改订单状态
         */
        order.setPaymentStatus("PAID");

        order.setOrderStatus("CONFIRMED");

        bookingOrderMapper.updateById(order);
        dailyCheckinService.markUsed(order.getOrderId());


        /*
         * 11. 返回更新后的订单
         */
        BookingOrderVO vo =
                toBookingOrderVO(order);


        /*
         * 查询当前订单全部入住人
         */
        List<BookingGuest> guests =
                bookingGuestMapper.selectList(
                        new LambdaQueryWrapper<BookingGuest>()
                                .eq(
                                        BookingGuest::getOrderId,
                                        order.getOrderId()
                                )
                                .orderByDesc(
                                        BookingGuest::getIsPrimary
                                )
                                .orderByAsc(
                                        BookingGuest::getGuestId
                                )
                );


        List<BookingGuestVO> guestVOList =
                new ArrayList<>();


        for (BookingGuest guest : guests) {

            BookingGuestVO guestVO =
                    new BookingGuestVO();

            guestVO.setGuestId(
                    guest.getGuestId()
            );

            guestVO.setGuestName(
                    guest.getGuestName()
            );

            guestVO.setPhone(
                    guest.getPhone()
            );

            guestVO.setIdType(
                    guest.getIdType()
            );

            guestVO.setIdNumber(
                    guest.getIdNumber()
            );

            guestVO.setIsPrimary(
                    guest.getIsPrimary()
            );

            guestVOList.add(
                    guestVO
            );
        }


        vo.setGuests(
                guestVOList
        );


        return vo;
    }

}
