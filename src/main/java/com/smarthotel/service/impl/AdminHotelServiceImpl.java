package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.Room;
import com.smarthotel.entity.RoomType;
import com.smarthotel.mapper.RoomMapper;
import com.smarthotel.mapper.RoomTypeMapper;
import com.smarthotel.service.AdminHotelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.entity.RoomPrice;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.RoomPriceMapper;
import com.smarthotel.exception.BusinessException;

import java.time.LocalDate;
import com.smarthotel.dto.admin.UpdateInventoryRequest;
import com.smarthotel.dto.admin.UpdatePriceRequest;
import com.smarthotel.service.InventoryRedisService;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminHotelServiceImpl
        implements AdminHotelService {

    private final RoomTypeMapper roomTypeMapper;

    private final RoomMapper roomMapper;

    private final RoomInventoryMapper roomInventoryMapper;

    private final RoomPriceMapper roomPriceMapper;

    private final InventoryRedisService inventoryRedisService;

    /**
     * 管理员查看全部房型
     *
     * 包括已停售房型
     */
    @Override
    public List<RoomType> listRoomTypes() {

        return roomTypeMapper.selectList(
                new LambdaQueryWrapper<RoomType>()
                        .orderByAsc(
                                RoomType::getSortNo
                        )
        );
    }


    /**
     * 管理员查看实体客房
     *
     * 可按房型和房态筛选
     */
    @Override
    public List<Room> listRooms(
            Long roomTypeId,
            String status) {

        LambdaQueryWrapper<Room> wrapper =
                new LambdaQueryWrapper<>();


        if (roomTypeId != null) {

            wrapper.eq(
                    Room::getRoomTypeId,
                    roomTypeId
            );
        }


        if (status != null
                && !status.isBlank()) {

            wrapper.eq(
                    Room::getStatus,
                    status
            );
        }


        wrapper
                .orderByAsc(Room::getFloorNo)
                .orderByAsc(Room::getRoomNo);


        return roomMapper.selectList(wrapper);
    }

    @Override
    public List<RoomInventory> listInventories(
            Long roomTypeId,
            LocalDate startDate,
            LocalDate endDate) {

        if (roomTypeId == null) {
            throw new BusinessException(
                    400,
                    "房型不能为空"
            );
        }

        if (startDate == null || endDate == null) {
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

        return roomInventoryMapper.selectList(
                new LambdaQueryWrapper<RoomInventory>()
                        .eq(
                                RoomInventory::getRoomTypeId,
                                roomTypeId
                        )
                        .ge(
                                RoomInventory::getInventoryDate,
                                startDate
                        )
                        .le(
                                RoomInventory::getInventoryDate,
                                endDate
                        )
                        .orderByAsc(
                                RoomInventory::getInventoryDate
                        )
        );
    }


    @Override
    public List<RoomPrice> listPrices(
            Long roomTypeId,
            LocalDate startDate,
            LocalDate endDate) {

        if (roomTypeId == null) {
            throw new BusinessException(
                    400,
                    "房型不能为空"
            );
        }

        if (startDate == null || endDate == null) {
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

        return roomPriceMapper.selectList(
                new LambdaQueryWrapper<RoomPrice>()
                        .eq(
                                RoomPrice::getRoomTypeId,
                                roomTypeId
                        )
                        .ge(
                                RoomPrice::getPriceDate,
                                startDate
                        )
                        .le(
                                RoomPrice::getPriceDate,
                                endDate
                        )
                        .orderByAsc(
                                RoomPrice::getPriceDate
                        )
        );
    }

    @Override
    @Transactional
    public void updateInventory(
            UpdateInventoryRequest request) {

        /*
         * 1. 锁住这一天的库存
         */
        RoomInventory inventory =
                roomInventoryMapper.selectOne(
                        new LambdaQueryWrapper<RoomInventory>()
                                .eq(
                                        RoomInventory::getRoomTypeId,
                                        request.getRoomTypeId()
                                )
                                .eq(
                                        RoomInventory::getInventoryDate,
                                        request.getInventoryDate()
                                )
                                .last("FOR UPDATE")
                );


        if (inventory == null) {

            throw new BusinessException(
                    404,
                    "该日期没有房量数据"
            );
        }


        /*
         * 2. 查询房型
         */
        RoomType roomType =
                roomTypeMapper.selectById(
                        request.getRoomTypeId()
                );


        if (roomType == null) {

            throw new BusinessException(
                    404,
                    "房型不存在"
            );
        }


        /*
         * 3. 房量不能超过真实实体房间数量
         *
         * 例如：
         * 豪华大床房只有70间，
         * totalQty不能改成100。
         */
        if (request.getTotalQty()
                > roomType.getTotalRooms()) {

            throw new BusinessException(
                    400,
                    "总房量不能超过该房型实体房数量"
            );
        }


        /*
         * 4. 不能小于已经占用的库存
         *
         * locked + sold 已经被订单占用。
         */
        int occupiedQty =
                inventory.getLockedQty()
                        + inventory.getSoldQty();


        if (request.getTotalQty()
                < occupiedQty) {

            throw new BusinessException(
                    400,
                    "总房量不能小于已锁定量与已售量之和"
            );
        }


        /*
         * 5. 修改MySQL
         */
        inventory.setTotalQty(
                request.getTotalQty()
        );

        roomInventoryMapper.updateById(
                inventory
        );


        /*
         * 6. 重新计算真正可售库存
         */
        int availableQty =
                inventory.getTotalQty()
                        - inventory.getLockedQty()
                        - inventory.getSoldQty();


        /*
         * 7. 同步Redis
         */
        inventoryRedisService.syncInventory(
                inventory.getRoomTypeId(),
                inventory.getInventoryDate(),
                availableQty
        );
    }

    @Override
    @Transactional
    public void updatePrice(
            UpdatePriceRequest request) {

        /*
         * 1. 锁住当天房价
         */
        RoomPrice price =
                roomPriceMapper.selectOne(
                        new LambdaQueryWrapper<RoomPrice>()
                                .eq(
                                        RoomPrice::getRoomTypeId,
                                        request.getRoomTypeId()
                                )
                                .eq(
                                        RoomPrice::getPriceDate,
                                        request.getPriceDate()
                                )
                                .last("FOR UPDATE")
                );


        if (price == null) {

            throw new BusinessException(
                    404,
                    "该日期没有房价数据"
            );
        }


        /*
         * 2. 修改每日售价
         */
        price.setSalePrice(
                request.getSalePrice()
        );


        roomPriceMapper.updateById(
                price
        );
    }

}