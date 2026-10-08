package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.RoomType;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.BookingGuestMapper;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.RoomTypeMapper;
import com.smarthotel.service.StaffOrderService;
import com.smarthotel.vo.StaffOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.smarthotel.entity.BookingGuest;
import com.smarthotel.vo.BookingGuestVO;

import com.smarthotel.entity.*;
import com.smarthotel.mapper.*;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffOrderServiceImpl
        implements StaffOrderService {

    private final BookingOrderMapper bookingOrderMapper;

    private final BookingGuestMapper bookingGuestMapper;

    private final RoomTypeMapper roomTypeMapper;


    @Override
    public List<StaffOrderVO> listOrders(
            String status) {

        LambdaQueryWrapper<BookingOrder> wrapper =
                new LambdaQueryWrapper<>();

        /*
         * 如果传了状态，就按状态过滤
         */
        if (status != null
                && !status.isBlank()) {

            wrapper.eq(
                    BookingOrder::getOrderStatus,
                    status
            );
        }

        wrapper.orderByDesc(
                BookingOrder::getCreateTime
        );


        List<BookingOrder> orders =
                bookingOrderMapper.selectList(
                        wrapper
                );


        List<StaffOrderVO> result =
                new ArrayList<>();


        for (BookingOrder order : orders) {

            StaffOrderVO vo =
                    new StaffOrderVO();

            vo.setOrderId(
                    order.getOrderId()
            );

            vo.setOrderNo(
                    order.getOrderNo()
            );

            vo.setRoomTypeId(
                    order.getRoomTypeId()
            );

            RoomType roomType =
                    roomTypeMapper.selectById(
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

            vo.setTotalAmount(
                    order.getTotalAmount()
            );

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

            vo.setCreateTime(
                    order.getCreateTime()
            );

            result.add(vo);
        }

        return result;
    }

    @Override
    public List<BookingGuestVO> listOrderGuests(
            Long orderId) {

        /*
         * 1. 确认订单存在
         */
        BookingOrder order =
                bookingOrderMapper.selectById(
                        orderId
                );

        if (order == null) {

            throw new BusinessException(
                    404,
                    "订单不存在"
            );
        }


        /*
         * 2. 查询入住人
         */
        List<BookingGuest> guests =
                bookingGuestMapper.selectList(
                        new LambdaQueryWrapper<BookingGuest>()
                                .eq(
                                        BookingGuest::getOrderId,
                                        orderId
                                )
                                .orderByDesc(
                                        BookingGuest::getIsPrimary
                                )
                                .orderByAsc(
                                        BookingGuest::getGuestId
                                )
                );


        /*
         * 3. Entity -> VO
         */
        List<BookingGuestVO> result =
                new ArrayList<>();


        for (BookingGuest guest : guests) {

            BookingGuestVO vo =
                    new BookingGuestVO();

            vo.setGuestId(
                    guest.getGuestId()
            );

            vo.setGuestName(
                    guest.getGuestName()
            );

            vo.setPhone(
                    guest.getPhone()
            );

            vo.setIdType(
                    guest.getIdType()
            );

            vo.setIdNumber(
                    guest.getIdNumber()
            );

            vo.setIsPrimary(
                    guest.getIsPrimary()
            );


            result.add(vo);
        }


        return result;
    }
}