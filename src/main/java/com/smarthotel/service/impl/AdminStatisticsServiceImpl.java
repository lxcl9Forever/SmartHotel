package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.*;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.*;
import com.smarthotel.service.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl
        implements AdminStatisticsService {

    private final HotelDailyStatisticsMapper
            hotelDailyStatisticsMapper;

    private final BookingOrderMapper
            bookingOrderMapper;

    private final CheckInRecordMapper
            checkInRecordMapper;

    private final CheckOutRecordMapper
            checkOutRecordMapper;

    private final RoomAssignmentMapper
            roomAssignmentMapper;

    private final RoomInventoryMapper
            roomInventoryMapper;

    private final PaymentMapper
            paymentMapper;

    private LocalDateTime getStartTime(
            LocalDate date) {

        return date.atStartOfDay();
    }


    private LocalDateTime getEndTime(
            LocalDate date) {

        return date
                .plusDays(1)
                .atStartOfDay();
    }

    private int countNewOrders(
            LocalDate date) {

        LocalDateTime start =
                getStartTime(date);

        LocalDateTime end =
                getEndTime(date);


        Long count =
                bookingOrderMapper.selectCount(
                        new LambdaQueryWrapper<BookingOrder>()
                                .ge(
                                        BookingOrder::getCreateTime,
                                        start
                                )
                                .lt(
                                        BookingOrder::getCreateTime,
                                        end
                                )
                );


        return count.intValue();
    }

    private int countCheckIns(
            LocalDate date) {

        LocalDateTime start =
                getStartTime(date);

        LocalDateTime end =
                getEndTime(date);


        Long count =
                checkInRecordMapper.selectCount(
                        new LambdaQueryWrapper<CheckInRecord>()
                                .ge(
                                        CheckInRecord::getActualCheckInTime,
                                        start
                                )
                                .lt(
                                        CheckInRecord::getActualCheckInTime,
                                        end
                                )
                );


        return count.intValue();
    }

    private int countCheckOuts(
            LocalDate date) {

        LocalDateTime start =
                getStartTime(date);

        LocalDateTime end =
                getEndTime(date);


        Long count =
                checkOutRecordMapper.selectCount(
                        new LambdaQueryWrapper<CheckOutRecord>()
                                .ge(
                                        CheckOutRecord::getActualCheckOutTime,
                                        start
                                )
                                .lt(
                                        CheckOutRecord::getActualCheckOutTime,
                                        end
                                )
                );


        return count.intValue();
    }

    private int calculateAvailableRooms(
            LocalDate date) {

        List<RoomInventory> inventories =
                roomInventoryMapper.selectList(
                        new LambdaQueryWrapper<RoomInventory>()
                                .eq(
                                        RoomInventory::getInventoryDate,
                                        date
                                )
                );


        int availableRoomCount = 0;


        for (RoomInventory inventory
                : inventories) {

            int available =
                    inventory.getTotalQty()
                            - inventory.getLockedQty()
                            - inventory.getSoldQty();


            /*
             * 做一下防御处理，
             * 避免异常数据导致负数
             */
            availableRoomCount +=
                    Math.max(available, 0);
        }


        return availableRoomCount;
    }

    private int calculateOccupiedRooms(
            LocalDate date) {

        LocalDateTime start =
                getStartTime(date);

        LocalDateTime end =
                getEndTime(date);


        /*
         * 查询在当天开始之前已经生效，
         * 或当天之内开始生效的分房记录
         */
        List<RoomAssignment> assignments =
                roomAssignmentMapper.selectList(
                        new LambdaQueryWrapper<RoomAssignment>()
                                .lt(
                                        RoomAssignment::getEffectiveFrom,
                                        end
                                )
                );


        int occupiedCount = 0;


        for (RoomAssignment assignment
                : assignments) {

            LocalDateTime effectiveTo =
                    assignment.getEffectiveTo();


            /*
             * 如果：
             *
             * effectiveTo == null
             *
             * 说明当天仍然在住。
             *
             * 或者：
             *
             * effectiveTo > 当天00:00
             *
             * 说明当天至少有一段时间处于入住状态。
             */
            if (
                    effectiveTo == null
                            ||
                            effectiveTo.isAfter(start)
            ) {

                occupiedCount++;
            }
        }


        return occupiedCount;
    }

    private BigDecimal calculateOccupancyRate(
            int occupiedRoomCount) {

        /*
         * 酒店固定354间实体客房
         */
        BigDecimal totalRooms =
                BigDecimal.valueOf(354);


        return BigDecimal
                .valueOf(
                        occupiedRoomCount
                )
                .divide(
                        totalRooms,
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal calculateRoomRevenue(
            LocalDate date) {

        LocalDateTime start =
                getStartTime(date);

        LocalDateTime end =
                getEndTime(date);


        List<Payment> payments =
                paymentMapper.selectList(
                        new LambdaQueryWrapper<Payment>()
                                .eq(
                                        Payment::getPaymentStatus,
                                        "SUCCESS"
                                )
                                .ge(
                                        Payment::getPaidTime,
                                        start
                                )
                                .lt(
                                        Payment::getPaidTime,
                                        end
                                )
                );


        BigDecimal revenue =
                BigDecimal.ZERO;


        for (Payment payment : payments) {

            if (payment.getAmount() != null) {

                revenue =
                        revenue.add(
                                payment.getAmount()
                        );
            }
        }


        return revenue;
    }

    private BigDecimal calculateExtraRevenue(
            LocalDate date) {

        LocalDateTime start =
                getStartTime(date);

        LocalDateTime end =
                getEndTime(date);


        List<CheckOutRecord> records =
                checkOutRecordMapper.selectList(
                        new LambdaQueryWrapper<CheckOutRecord>()
                                .ge(
                                        CheckOutRecord::getActualCheckOutTime,
                                        start
                                )
                                .lt(
                                        CheckOutRecord::getActualCheckOutTime,
                                        end
                                )
                );


        BigDecimal extraRevenue =
                BigDecimal.ZERO;


        for (CheckOutRecord record : records) {

            if (record.getExtraCharge() != null) {

                extraRevenue =
                        extraRevenue.add(
                                record.getExtraCharge()
                        );
            }
        }


        return extraRevenue;
    }

    @Override
    @Transactional
    public void refreshDailyStatistics(
            LocalDate date) {

        if (date == null) {

            throw new BusinessException(
                    400,
                    "统计日期不能为空"
            );
        }


        /*
         * 1. 计算各项指标
         */
        int newOrderCount =
                countNewOrders(date);


        int checkInCount =
                countCheckIns(date);


        int checkOutCount =
                countCheckOuts(date);


        int occupiedRoomCount =
                calculateOccupiedRooms(date);


        int availableRoomCount =
                calculateAvailableRooms(date);


        BigDecimal occupancyRate =
                calculateOccupancyRate(
                        occupiedRoomCount
                );


        BigDecimal roomRevenue =
                calculateRoomRevenue(date);


        BigDecimal extraRevenue =
                calculateExtraRevenue(date);


        /*
         * 当前退款模块还没有做，
         * 暂时记0
         */
        BigDecimal refundAmount =
                BigDecimal.ZERO;


        BigDecimal totalRevenue =
                roomRevenue
                        .add(extraRevenue)
                        .subtract(refundAmount);


        /*
         * 2. 看这一天是否已经有统计记录
         */
        HotelDailyStatistics statistics =
                hotelDailyStatisticsMapper
                        .selectOne(
                                new LambdaQueryWrapper
                                        <HotelDailyStatistics>()
                                        .eq(
                                                HotelDailyStatistics::getStatDate,
                                                date
                                        )
                        );


        /*
         * 3. 没有就创建
         */
        if (statistics == null) {

            statistics =
                    new HotelDailyStatistics();

            statistics.setStatDate(date);

            statistics.setCreateTime(
                    LocalDateTime.now()
            );
        }


        /*
         * 4. 设置最新数据
         */
        statistics.setNewOrderCount(
                newOrderCount
        );

        statistics.setCheckInCount(
                checkInCount
        );

        statistics.setCheckOutCount(
                checkOutCount
        );

        statistics.setOccupiedRoomCount(
                occupiedRoomCount
        );

        statistics.setAvailableRoomCount(
                availableRoomCount
        );

        statistics.setOccupancyRate(
                occupancyRate
        );

        statistics.setRoomRevenue(
                roomRevenue
        );

        statistics.setExtraRevenue(
                extraRevenue
        );

        statistics.setRefundAmount(
                refundAmount
        );

        statistics.setTotalRevenue(
                totalRevenue
        );


        /*
         * 5. 新增或更新
         */
        if (statistics.getStatId() == null) {

            hotelDailyStatisticsMapper.insert(
                    statistics
            );

        } else {

            hotelDailyStatisticsMapper.updateById(
                    statistics
            );
        }
    }

    @Override
    @Transactional
    public void refreshStatisticsRange(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null
                || endDate == null) {

            throw new BusinessException(
                    400,
                    "开始日期和结束日期不能为空"
            );
        }


        if (startDate.isAfter(endDate)) {

            throw new BusinessException(
                    400,
                    "开始日期不能晚于结束日期"
            );
        }


        LocalDate currentDate =
                startDate;


        while (!currentDate.isAfter(endDate)) {

            refreshDailyStatistics(
                    currentDate
            );

            currentDate =
                    currentDate.plusDays(1);
        }
    }

    @Override
    public List<HotelDailyStatistics>
    listDailyStatistics(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null
                || endDate == null) {

            throw new BusinessException(
                    400,
                    "开始日期和结束日期不能为空"
            );
        }


        if (startDate.isAfter(endDate)) {

            throw new BusinessException(
                    400,
                    "开始日期不能晚于结束日期"
            );
        }


        return hotelDailyStatisticsMapper
                .selectList(
                        new LambdaQueryWrapper
                                <HotelDailyStatistics>()
                                .ge(
                                        HotelDailyStatistics::getStatDate,
                                        startDate
                                )
                                .le(
                                        HotelDailyStatistics::getStatDate,
                                        endDate
                                )
                                .orderByAsc(
                                        HotelDailyStatistics::getStatDate
                                )
                );
    }
}