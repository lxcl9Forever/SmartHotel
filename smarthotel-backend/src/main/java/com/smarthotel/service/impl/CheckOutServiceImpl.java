package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.checkout.CheckOutRequest;
import com.smarthotel.entity.*;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.*;
import com.smarthotel.service.CheckOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckOutServiceImpl
        implements CheckOutService {

    private final SysUserMapper sysUserMapper;

    private final BookingOrderMapper bookingOrderMapper;

    private final RoomAssignmentMapper roomAssignmentMapper;

    private final CheckInRecordMapper checkInRecordMapper;

    private final CheckOutRecordMapper checkOutRecordMapper;

    private final RoomMapper roomMapper;

    private final RoomStatusLogMapper roomStatusLogMapper;


    @Override
    @Transactional
    public void checkOut(
            String username,
            CheckOutRequest request) {

        /*
         * 1. 当前操作员工
         */
        SysUser staff =
                sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getUsername,
                                        username
                                )
                );

        if (staff == null) {
            throw new BusinessException(
                    401,
                    "当前员工账号不存在"
            );
        }


        /*
         * 2. 锁住订单
         */
        BookingOrder order =
                bookingOrderMapper.selectOne(
                        new LambdaQueryWrapper<BookingOrder>()
                                .eq(
                                        BookingOrder::getOrderId,
                                        request.getOrderId()
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
         * 3. 只有已入住订单才能退房
         */
        if (!"CHECKED_IN".equals(
                order.getOrderStatus())) {

            throw new BusinessException(
                    400,
                    "只有已入住订单才能办理退房"
            );
        }


        /*
         * 4. 查询该订单当前有效的分房记录
         */
        List<RoomAssignment> assignments =
                roomAssignmentMapper.selectList(
                        new LambdaQueryWrapper<RoomAssignment>()
                                .eq(
                                        RoomAssignment::getOrderId,
                                        order.getOrderId()
                                )
                                .eq(
                                        RoomAssignment::getAssignmentStatus,
                                        "ACTIVE"
                                )
                                .last("FOR UPDATE")
                );


        if (assignments.size()
                != order.getRoomCount()) {

            throw new BusinessException(
                    500,
                    "当前有效分房记录数量异常"
            );
        }


        /*
         * 每间房对应的房费
         *
         * 当前订单所有房间价格相同，
         * 所以直接平均分摊。
         */
        BigDecimal roomChargePerRoom =
                order.getTotalAmount()
                        .divide(
                                BigDecimal.valueOf(
                                        order.getRoomCount()
                                ),
                                2,
                                RoundingMode.HALF_UP
                        );


        LocalDateTime now =
                LocalDateTime.now();


        /*
         * 5. 一间房一间房退
         */
        for (RoomAssignment assignment
                : assignments) {

            /*
             * 5.1 找入住记录
             */
            CheckInRecord checkInRecord =
                    checkInRecordMapper.selectOne(
                            new LambdaQueryWrapper<CheckInRecord>()
                                    .eq(
                                            CheckInRecord::getAssignmentId,
                                            assignment.getAssignmentId()
                                    )
                    );

            if (checkInRecord == null) {
                throw new BusinessException(
                        500,
                        "入住记录不存在"
                );
            }


            /*
             * 防止重复退房
             */
            Long checkoutCount =
                    checkOutRecordMapper.selectCount(
                            new LambdaQueryWrapper<CheckOutRecord>()
                                    .eq(
                                            CheckOutRecord::getCheckInId,
                                            checkInRecord.getCheckInId()
                                    )
                    );

            if (checkoutCount > 0) {
                throw new BusinessException(
                        400,
                        "该房间已经办理退房"
                );
            }


            /*
             * 5.2 锁住房间
             */
            Room room =
                    roomMapper.selectOne(
                            new LambdaQueryWrapper<Room>()
                                    .eq(
                                            Room::getRoomId,
                                            assignment.getRoomId()
                                    )
                                    .last("FOR UPDATE")
                    );

            if (room == null) {
                throw new BusinessException(
                        500,
                        "实体房间不存在"
                );
            }


            if (!"OCCUPIED".equals(
                    room.getStatus())) {

                throw new BusinessException(
                        400,
                        "当前房态不能办理退房"
                );
            }


            /*
             * 5.3 创建退房记录
             */
            CheckOutRecord checkOutRecord =
                    new CheckOutRecord();

            checkOutRecord.setCheckInId(
                    checkInRecord.getCheckInId()
            );

            checkOutRecord.setActualCheckOutTime(
                    now
            );

            checkOutRecord.setRoomCharge(
                    roomChargePerRoom
            );

            checkOutRecord.setExtraCharge(
                    BigDecimal.ZERO
            );

            checkOutRecord.setRefundDeposit(
                    BigDecimal.ZERO
            );

            checkOutRecord.setSettlementAmount(
                    roomChargePerRoom
            );

            checkOutRecord.setHandledBy(
                    staff.getUserId()
            );

            checkOutRecord.setRemark(
                    request.getRemark()
            );

            checkOutRecordMapper.insert(
                    checkOutRecord
            );


            /*
             * 5.4 结束分房关系
             */
            assignment.setAssignmentStatus(
                    "ENDED"
            );

            assignment.setEffectiveTo(now);

            roomAssignmentMapper.updateById(
                    assignment
            );


            /*
             * 5.5 房态日志
             *
             * OCCUPIED → DIRTY
             */
            RoomStatusLog statusLog =
                    new RoomStatusLog();

            statusLog.setRoomId(
                    room.getRoomId()
            );

            statusLog.setOldStatus(
                    room.getStatus()
            );

            statusLog.setNewStatus(
                    "DIRTY"
            );

            statusLog.setChangeReason(
                    "办理退房"
            );

            statusLog.setOperatorUserId(
                    staff.getUserId()
            );

            statusLog.setChangeTime(now);

            roomStatusLogMapper.insert(
                    statusLog
            );


            /*
             * 5.6 当前房态变成DIRTY
             */
            room.setStatus(
                    "DIRTY"
            );

            roomMapper.updateById(
                    room
            );
        }


        /*
         * 6. 订单状态
         *
         * CHECKED_IN → CHECKED_OUT
         */
        order.setOrderStatus(
                "CHECKED_OUT"
        );

        bookingOrderMapper.updateById(
                order
        );
    }
}
