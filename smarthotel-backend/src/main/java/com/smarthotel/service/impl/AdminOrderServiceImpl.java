package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.admin.AdminOrderQuery;
import com.smarthotel.entity.BookingOrder;
import com.smarthotel.entity.RoomType;
import com.smarthotel.mapper.BookingOrderMapper;
import com.smarthotel.mapper.RoomTypeMapper;
import com.smarthotel.service.AdminOrderService;
import com.smarthotel.vo.AdminOrderVO;
import com.smarthotel.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.smarthotel.entity.BookingGuest;
import com.smarthotel.mapper.BookingGuestMapper;
import com.smarthotel.vo.AdminOrderDetailVO;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl
        implements AdminOrderService {

    private final BookingOrderMapper bookingOrderMapper;

    private final RoomTypeMapper roomTypeMapper;

    private final BookingGuestMapper bookingGuestMapper;

    @Override
    public PageResult<AdminOrderVO> listOrders(
            AdminOrderQuery query
    ) {

        /*
         * 1. 防止非法分页参数
         */
        int page =
                query.getPage() == null
                        || query.getPage() < 1
                        ? 1
                        : query.getPage();

        int pageSize =
                query.getPageSize() == null
                        || query.getPageSize() < 1
                        ? 10
                        : Math.min(
                        query.getPageSize(),
                        100
                );


        /*
         * 2. 查询符合条件的总数量
         */
        LambdaQueryWrapper<BookingOrder>
                countWrapper =
                buildQueryWrapper(query);

        Long total =
                bookingOrderMapper.selectCount(
                        countWrapper
                );


        /*
         * 没有数据直接返回
         */
        if (total == 0) {

            return new PageResult<>(
                    0L,
                    page,
                    pageSize,
                    Collections.emptyList()
            );
        }


        /*
         * 3. 查询当前页数据
         */
        long offset =
                (long) (page - 1)
                        * pageSize;

        LambdaQueryWrapper<BookingOrder>
                listWrapper =
                buildQueryWrapper(query);

        listWrapper
                .orderByDesc(
                        BookingOrder::getCreateTime
                )
                .last(
                        "LIMIT "
                                + offset
                                + ", "
                                + pageSize
                );


        List<BookingOrder> orders =
                bookingOrderMapper.selectList(
                        listWrapper
                );


        /*
         * 4. 一次性查询当前页涉及的房型
         *
         * 不要每一个订单单独查询房型，
         * 避免 N+1 查询。
         */
        Set<Long> roomTypeIds =
                orders.stream()
                        .map(
                                BookingOrder::
                                        getRoomTypeId
                        )
                        .filter(
                                roomTypeId ->
                                        roomTypeId != null
                        )
                        .collect(
                                Collectors.toSet()
                        );


        Map<Long, RoomType>
                roomTypeMap;

        if (roomTypeIds.isEmpty()) {

            roomTypeMap =
                    Collections.emptyMap();

        } else {

            roomTypeMap =
                    roomTypeMapper
                            .selectBatchIds(
                                    roomTypeIds
                            )
                            .stream()
                            .collect(
                                    Collectors.toMap(
                                            RoomType::
                                                    getRoomTypeId,

                                            Function.identity()
                                    )
                            );
        }


        /*
         * 5. Entity 转换为 VO
         */
        List<AdminOrderVO> records =
                orders.stream()
                        .map(order -> {

                            AdminOrderVO vo =
                                    new AdminOrderVO();

                            BeanUtils.copyProperties(
                                    order,
                                    vo
                            );


                            RoomType roomType =
                                    roomTypeMap.get(
                                            order.getRoomTypeId()
                                    );

                            if (roomType != null) {

                                vo.setRoomTypeName(
                                        roomType.getTypeName()
                                );
                            }


                            return vo;

                        })
                        .toList();


        /*
         * 6. 返回分页结果
         */
        return new PageResult<>(
                total,
                page,
                pageSize,
                records
        );
    }


    @Override
    public AdminOrderDetailVO getOrderDetail(
            Long orderId
    ) {

        /*
         * 1. 查询订单
         */
        BookingOrder order =
                bookingOrderMapper.selectById(orderId);

        if (order == null) {
            throw new RuntimeException(
                    "订单不存在"
            );
        }


        /*
         * 2. 组装订单 VO
         */
        AdminOrderVO orderVO =
                new AdminOrderVO();

        BeanUtils.copyProperties(
                order,
                orderVO
        );


        /*
         * 3. 查询房型名称
         */
        if (order.getRoomTypeId() != null) {

            RoomType roomType =
                    roomTypeMapper.selectById(
                            order.getRoomTypeId()
                    );

            if (roomType != null) {

                orderVO.setRoomTypeName(
                        roomType.getTypeName()
                );
            }
        }


        /*
         * 4. 查询入住人
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
         * 5. 返回详情
         */
        AdminOrderDetailVO detail =
                new AdminOrderDetailVO();

        detail.setOrder(orderVO);
        detail.setGuests(guests);

        return detail;
    }

    /**
     * 构造订单查询条件
     */
    private LambdaQueryWrapper<BookingOrder>
    buildQueryWrapper(
            AdminOrderQuery query
    ) {

        LambdaQueryWrapper<BookingOrder>
                wrapper =
                new LambdaQueryWrapper<>();


        /*
         * 订单号模糊查询
         */
        if (
                StringUtils.hasText(
                        query.getOrderNo()
                )
        ) {

            wrapper.like(
                    BookingOrder::getOrderNo,
                    query.getOrderNo().trim()
            );
        }


        /*
         * 订单状态
         */
        if (
                StringUtils.hasText(
                        query.getOrderStatus()
                )
        ) {

            wrapper.eq(
                    BookingOrder::getOrderStatus,
                    query.getOrderStatus()
            );
        }


        /*
         * 支付状态
         */
        if (
                StringUtils.hasText(
                        query.getPaymentStatus()
                )
        ) {

            wrapper.eq(
                    BookingOrder::getPaymentStatus,
                    query.getPaymentStatus()
            );
        }


        /*
         * 入住日期
         */
        if (
                query.getCheckInDate()
                        != null
        ) {

            wrapper.eq(
                    BookingOrder::getCheckInDate,
                    query.getCheckInDate()
            );
        }


        return wrapper;
    }
}