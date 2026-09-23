package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.booking.CreateBookingRequest;
import com.smarthotel.entity.*;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.*;
import com.smarthotel.service.BookingService;
import com.smarthotel.vo.BookingOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final SysUserMapper sysUserMapper;

    private final CustomerMapper customerMapper;

    private final RoomTypeMapper roomTypeMapper;

    private final RoomInventoryMapper roomInventoryMapper;

    private final RoomPriceMapper roomPriceMapper;

    private final BookingOrderMapper bookingOrderMapper;

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
         * 6. 查询并锁住入住期间库存行
         *
         * FOR UPDATE 非常关键：
         * 当前事务结束前，其他下单事务不能同时修改这些库存行。
         */
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

        order.setTotalAmount(totalAmount);

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
                        .plusMinutes(15)
        );

        order.setRemark(
                request.getRemark()
        );

        bookingOrderMapper.insert(order);


        /*
         * 12. 返回订单信息
         */
        BookingOrderVO vo =
                new BookingOrderVO();

        vo.setOrderId(order.getOrderId());
        vo.setOrderNo(order.getOrderNo());
        vo.setRoomTypeName(
                roomType.getTypeName()
        );
        vo.setRoomCount(order.getRoomCount());
        vo.setGuestCount(order.getGuestCount());
        vo.setCheckInDate(order.getCheckInDate());
        vo.setCheckOutDate(order.getCheckOutDate());
        vo.setNights(order.getNights());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setPaymentStatus(order.getPaymentStatus());
        vo.setExpireTime(order.getExpireTime());

        return vo;
    }
}