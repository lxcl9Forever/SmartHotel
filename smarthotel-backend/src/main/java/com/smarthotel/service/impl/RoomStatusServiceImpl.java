package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.Room;
import com.smarthotel.entity.RoomStatusLog;
import com.smarthotel.entity.SysUser;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.RoomMapper;
import com.smarthotel.mapper.RoomStatusLogMapper;
import com.smarthotel.mapper.SysUserMapper;
import com.smarthotel.service.RoomStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoomStatusServiceImpl
        implements RoomStatusService {

    private final RoomMapper roomMapper;

    private final RoomStatusLogMapper roomStatusLogMapper;

    private final SysUserMapper sysUserMapper;


    /**
     * DIRTY → CLEANING
     */
    @Override
    @Transactional
    public void startCleaning(
            String username,
            Long roomId) {

        SysUser user = getCurrentUser(username);

        /*
         * 锁住具体房间
         */
        Room room =
                roomMapper.selectOne(
                        new LambdaQueryWrapper<Room>()
                                .eq(
                                        Room::getRoomId,
                                        roomId
                                )
                                .last("FOR UPDATE")
                );

        if (room == null) {
            throw new BusinessException(
                    404,
                    "房间不存在"
            );
        }


        if (!"DIRTY".equals(
                room.getStatus())) {

            throw new BusinessException(
                    400,
                    "只有待清洁房间才能开始清洁"
            );
        }


        /*
         * 写房态日志
         */
        saveStatusLog(
                room,
                "CLEANING",
                "开始客房清洁",
                user.getUserId()
        );


        /*
         * 修改当前房态
         */
        room.setStatus("CLEANING");

        roomMapper.updateById(room);
    }


    /**
     * CLEANING → VACANT
     */
    @Override
    @Transactional
    public void finishCleaning(
            String username,
            Long roomId) {

        SysUser user = getCurrentUser(username);


        Room room =
                roomMapper.selectOne(
                        new LambdaQueryWrapper<Room>()
                                .eq(
                                        Room::getRoomId,
                                        roomId
                                )
                                .last("FOR UPDATE")
                );

        if (room == null) {
            throw new BusinessException(
                    404,
                    "房间不存在"
            );
        }


        if (!"CLEANING".equals(
                room.getStatus())) {

            throw new BusinessException(
                    400,
                    "只有清洁中的房间才能完成清洁"
            );
        }


        /*
         * CLEANING → VACANT
         */
        saveStatusLog(
                room,
                "VACANT",
                "客房清洁完成",
                user.getUserId()
        );


        room.setStatus("VACANT");

        roomMapper.updateById(room);
    }


    /**
     * 当前员工账号
     */
    private SysUser getCurrentUser(
            String username) {

        SysUser user =
                sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getUsername,
                                        username
                                )
                );

        if (user == null) {
            throw new BusinessException(
                    401,
                    "当前员工账号不存在"
            );
        }

        return user;
    }


    /**
     * 保存房态变更日志
     */
    private void saveStatusLog(
            Room room,
            String newStatus,
            String reason,
            Long operatorUserId) {

        RoomStatusLog log =
                new RoomStatusLog();

        log.setRoomId(
                room.getRoomId()
        );

        log.setOldStatus(
                room.getStatus()
        );

        log.setNewStatus(
                newStatus
        );

        log.setChangeReason(
                reason
        );

        log.setOperatorUserId(
                operatorUserId
        );

        log.setChangeTime(
                LocalDateTime.now()
        );

        roomStatusLogMapper.insert(log);
    }

    @Override
    @Transactional
    public void startMaintenance(
            String username,
            Long roomId,
            String reason) {

        SysUser operator =
                sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getUsername,
                                        username
                                )
                );

        if (operator == null) {
            throw new BusinessException(
                    401,
                    "当前员工账号不存在"
            );
        }


        Room room =
                roomMapper.selectOne(
                        new LambdaQueryWrapper<Room>()
                                .eq(
                                        Room::getRoomId,
                                        roomId
                                )
                                .last("FOR UPDATE")
                );

        if (room == null) {
            throw new BusinessException(
                    404,
                    "房间不存在"
            );
        }


        /*
         * 当前版本只有空闲房可以进入维修
         */
        if (!"VACANT".equals(
                room.getStatus())) {

            throw new BusinessException(
                    400,
                    "只有空闲房间才能进入维修状态"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        /*
         * 写房态日志
         */
        RoomStatusLog log =
                new RoomStatusLog();

        log.setRoomId(
                room.getRoomId()
        );

        log.setOldStatus(
                room.getStatus()
        );

        log.setNewStatus(
                "MAINTENANCE"
        );

        log.setChangeReason(
                reason
        );

        log.setOperatorUserId(
                operator.getUserId()
        );

        log.setChangeTime(now);

        roomStatusLogMapper.insert(log);


        /*
         * 修改房态
         */
        room.setStatus(
                "MAINTENANCE"
        );

        room.setRemark(reason);

        room.setUpdateTime(now);

        roomMapper.updateById(room);
    }

    @Override
    @Transactional
    public void setOutOfService(
            String username,
            Long roomId,
            String reason) {

        SysUser operator =
                sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getUsername,
                                        username
                                )
                );

        if (operator == null) {
            throw new BusinessException(
                    401,
                    "当前员工账号不存在"
            );
        }


        Room room =
                roomMapper.selectOne(
                        new LambdaQueryWrapper<Room>()
                                .eq(
                                        Room::getRoomId,
                                        roomId
                                )
                                .last("FOR UPDATE")
                );

        if (room == null) {
            throw new BusinessException(
                    404,
                    "房间不存在"
            );
        }


        if (!"VACANT".equals(
                room.getStatus())) {

            throw new BusinessException(
                    400,
                    "只有空闲房间才能停用"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        RoomStatusLog log =
                new RoomStatusLog();

        log.setRoomId(
                room.getRoomId()
        );

        log.setOldStatus(
                room.getStatus()
        );

        log.setNewStatus(
                "OUT_OF_SERVICE"
        );

        log.setChangeReason(
                reason
        );

        log.setOperatorUserId(
                operator.getUserId()
        );

        log.setChangeTime(now);

        roomStatusLogMapper.insert(log);


        room.setStatus(
                "OUT_OF_SERVICE"
        );

        room.setRemark(reason);

        room.setUpdateTime(now);

        roomMapper.updateById(room);
    }

    @Override
    @Transactional
    public void restoreRoom(
            String username,
            Long roomId,
            String reason) {

        SysUser operator =
                sysUserMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(
                                        SysUser::getUsername,
                                        username
                                )
                );

        if (operator == null) {
            throw new BusinessException(
                    401,
                    "当前员工账号不存在"
            );
        }


        Room room =
                roomMapper.selectOne(
                        new LambdaQueryWrapper<Room>()
                                .eq(
                                        Room::getRoomId,
                                        roomId
                                )
                                .last("FOR UPDATE")
                );

        if (room == null) {
            throw new BusinessException(
                    404,
                    "房间不存在"
            );
        }


        String oldStatus =
                room.getStatus();


        if (
                !"MAINTENANCE".equals(oldStatus)
                        &&
                        !"OUT_OF_SERVICE".equals(oldStatus)
        ) {

            throw new BusinessException(
                    400,
                    "当前房态不能恢复为空闲"
            );
        }


        LocalDateTime now =
                LocalDateTime.now();


        RoomStatusLog log =
                new RoomStatusLog();

        log.setRoomId(
                room.getRoomId()
        );

        log.setOldStatus(
                oldStatus
        );

        log.setNewStatus(
                "VACANT"
        );

        log.setChangeReason(
                reason
        );

        log.setOperatorUserId(
                operator.getUserId()
        );

        log.setChangeTime(now);

        roomStatusLogMapper.insert(log);


        roomMapper.update(
                null,
                new LambdaUpdateWrapper<Room>()
                        .eq(
                                Room::getRoomId,
                                room.getRoomId()
                        )
                        .set(
                                Room::getStatus,
                                "VACANT"
                        )
                        .set(
                                Room::getRemark,
                                null
                        )
                        .set(
                                Room::getUpdateTime,
                                now
                        )
        );
    }

}