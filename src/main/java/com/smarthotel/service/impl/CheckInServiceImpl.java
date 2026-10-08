package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.dto.checkin.CheckInRequest;
import com.smarthotel.entity.*;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.*;
import com.smarthotel.service.CheckInService;
import com.smarthotel.vo.AvailablePhysicalRoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl
        implements CheckInService {

    private final BookingOrderMapper bookingOrderMapper;

    private final RoomMapper roomMapper;

    private final SysUserMapper sysUserMapper;

    private final RoomAssignmentMapper roomAssignmentMapper;

    private final CheckInRecordMapper checkInRecordMapper;

    private final RoomStatusLogMapper roomStatusLogMapper;

    private final BookingGuestMapper bookingGuestMapper;

    /**
     * 根据订单查询可以分配的具体房间
     */
    @Override
    public List<AvailablePhysicalRoomVO>
    listAvailableRooms(Long orderId) {

        /*
         * 1. 查询订单
         */
        BookingOrder order =
                bookingOrderMapper.selectById(orderId);

        if (order == null) {
            throw new BusinessException(
                    404,
                    "订单不存在"
            );
        }


        /*
         * 2. 必须已经确认
         */
        if (!"CONFIRMED".equals(
                order.getOrderStatus())) {

            throw new BusinessException(
                    400,
                    "只有已确认订单才能办理入住"
            );
        }


        /*
         * 3. 必须已经支付
         */
        if (!"PAID".equals(
                order.getPaymentStatus())) {

            throw new BusinessException(
                    400,
                    "订单尚未支付"
            );
        }


        /*
         * 4. 查询：
         *
         * 同一个房型
         * 已启用
         * VACANT空闲
         */
        List<Room> rooms =
                roomMapper.selectList(
                        new LambdaQueryWrapper<Room>()
                                .eq(
                                        Room::getRoomTypeId,
                                        order.getRoomTypeId()
                                )
                                .eq(
                                        Room::getStatus,
                                        "VACANT"
                                )
                                .orderByAsc(
                                        Room::getFloorNo
                                )
                                .orderByAsc(
                                        Room::getRoomNo
                                )
                );

        /*
         * 5. Entity → VO
         */
        List<AvailablePhysicalRoomVO> result =
                new ArrayList<>();

        for (Room room : rooms) {

            AvailablePhysicalRoomVO vo =
                    new AvailablePhysicalRoomVO();

            vo.setRoomId(
                    room.getRoomId()
            );

            vo.setRoomNo(
                    room.getRoomNo()
            );

            vo.setFloorNo(
                    room.getFloorNo()
            );

            vo.setStatus(
                    room.getStatus()
            );

            result.add(vo);
        }


        return result;
    }


    /**
     * STAFF办理入住
     */
    @Override
    @Transactional
    public void checkIn(
            String username,
            CheckInRequest request) {

        /*
         * 1. 找当前操作员工账号
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
         * 3. 状态校验
         */
        if (!"CONFIRMED".equals(
                order.getOrderStatus())) {

            throw new BusinessException(
                    400,
                    "当前订单状态不能办理入住"
            );
        }

        if (!"PAID".equals(
                order.getPaymentStatus())) {

            throw new BusinessException(
                    400,
                    "订单尚未支付"
            );
        }


        /*
         * 正式酒店业务：
         * 不允许提前很多天办理入住。
         *
         * 当前先要求必须是订单入住当天。
         */
        if (!LocalDate.now().equals(
                order.getCheckInDate())) {

            throw new BusinessException(
                    400,
                    "只能在订单入住日期办理入住"
            );
        }


        /*
         * 4. 房间数量必须一致
         *
         * 订单订了2间，
         * 就必须选2个具体房号。
         */
        if (request.getRoomIds().size()
                != order.getRoomCount()) {

            throw new BusinessException(
                    400,
                    "分配房间数量必须与订单房间数量一致"
            );
        }


        /*
         * 5. 防止用户传重复roomId
         *
         * 例如：
         * [101, 101]
         */
        Set<Long> uniqueRoomIds =
                new HashSet<>(
                        request.getRoomIds()
                );

        if (uniqueRoomIds.size()
                != request.getRoomIds().size()) {

            throw new BusinessException(
                    400,
                    "不能重复选择同一房间"
            );
        }


        /*
         * 6. 查询并锁住实体房间
         */
        List<Room> rooms =
                roomMapper.selectList(
                        new LambdaQueryWrapper<Room>()
                                .in(
                                        Room::getRoomId,
                                        request.getRoomIds()
                                )
                                .last("FOR UPDATE")
                );


        if (rooms.size()
                != request.getRoomIds().size()) {

            throw new BusinessException(
                    400,
                    "存在无效房间"
            );
        }


        /*
         * 7. 再次验证每个房间
         *
         * 防止两个前台同时给两个客户分配同一个房间。
         */
        for (Room room : rooms) {

            /*
             * 必须属于订单所订房型
             */
            if (!Objects.equals(
                    room.getRoomTypeId(),
                    order.getRoomTypeId())) {

                throw new BusinessException(
                        400,
                        "所选房间与订单房型不一致"
                );
            }

            /*
             * 房间必须VACANT
             */
            if (!"VACANT".equals(
                    room.getStatus())) {

                throw new BusinessException(
                        409,
                        "所选房间已不可入住"
                );
            }
        }


        /*
         * 查询订单主要入住人
         */
        BookingGuest primaryGuest =
                bookingGuestMapper.selectOne(
                        new LambdaQueryWrapper<BookingGuest>()
                                .eq(
                                        BookingGuest::getOrderId,
                                        order.getOrderId()
                                )
                                .eq(
                                        BookingGuest::getIsPrimary,
                                        1
                                )
                                .last("LIMIT 1")
                );


        if (primaryGuest == null) {

            throw new BusinessException(
                    400,
                    "订单缺少主要入住人信息"
            );
        }

        /*
         * 押金
         */
        BigDecimal depositPerRoom =
                request.getDepositPerRoom() == null
                        ? BigDecimal.ZERO
                        : request.getDepositPerRoom();


        LocalDateTime now =
                LocalDateTime.now();


        /*
         * 8. 给每一个实体房间：
         *
         * 创建分房记录
         * 创建入住记录
         * 修改房态
         * 写房态日志
         */
        for (Room room : rooms) {

            /*
             * 8.1 分房记录
             */
            RoomAssignment assignment =
                    new RoomAssignment();

            assignment.setOrderId(
                    order.getOrderId()
            );

            assignment.setRoomId(
                    room.getRoomId()
            );

            assignment.setAssignedBy(
                    staff.getUserId()
            );

            assignment.setAssignTime(now);

            assignment.setEffectiveFrom(now);

            assignment.setAssignmentStatus(
                    "ACTIVE"
            );

            roomAssignmentMapper.insert(
                    assignment
            );


            /*
             * 8.2 入住记录
             *
             * 当前 booking_guest
             * 还没有在客户预订页面完善，
             * 所以 primaryGuestId 暂时为空。
             */
            CheckInRecord checkInRecord =
                    new CheckInRecord();

            checkInRecord.setAssignmentId(
                    assignment.getAssignmentId()
            );

            checkInRecord.setPrimaryGuestId(
                    primaryGuest.getGuestId()
            );

            checkInRecord.setActualCheckInTime(
                    now
            );

            checkInRecord.setDepositAmount(
                    depositPerRoom
            );

            checkInRecord.setHandledBy(
                    staff.getUserId()
            );

            checkInRecord.setRemark(
                    request.getRemark()
            );

            checkInRecord.setCreateTime(
                    now
            );

            checkInRecordMapper.insert(
                    checkInRecord
            );


            /*
             * 8.3 房态日志
             *
             * VACANT → OCCUPIED
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
                    "OCCUPIED"
            );

            statusLog.setChangeReason(
                    "办理入住"
            );

            statusLog.setOperatorUserId(
                    staff.getUserId()
            );

            statusLog.setChangeTime(
                    now
            );

            roomStatusLogMapper.insert(
                    statusLog
            );


            /*
             * 8.4 修改当前物理房态
             */
            room.setStatus(
                    "OCCUPIED"
            );

            roomMapper.updateById(
                    room
            );
        }


        /*
         * 9. 修改订单状态
         *
         * CONFIRMED → CHECKED_IN
         */
        order.setOrderStatus(
                "CHECKED_IN"
        );

        bookingOrderMapper.updateById(
                order
        );
    }
}