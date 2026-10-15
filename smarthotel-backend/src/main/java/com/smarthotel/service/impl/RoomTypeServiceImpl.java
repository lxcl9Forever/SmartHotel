package com.smarthotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.entity.RoomInventory;
import com.smarthotel.entity.RoomPrice;
import com.smarthotel.entity.RoomType;
import com.smarthotel.exception.BusinessException;
import com.smarthotel.mapper.RoomInventoryMapper;
import com.smarthotel.mapper.RoomPriceMapper;
import com.smarthotel.mapper.RoomTypeMapper;
import com.smarthotel.service.RoomTypeService;
import com.smarthotel.vo.AvailableRoomTypeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeMapper roomTypeMapper;

    private final RoomInventoryMapper roomInventoryMapper;

    private final RoomPriceMapper roomPriceMapper;


    /**
     * 查询所有正常销售的房型
     */
    @Override
    public List<RoomType> listAvailableRoomTypes() {

        return roomTypeMapper.selectList(
                new LambdaQueryWrapper<RoomType>()
                        .eq(RoomType::getStatus, 1)
                        .orderByAsc(RoomType::getSortNo)
        );
    }


    /**
     * 根据房型ID查询房型详情
     */
    @Override
    public RoomType getRoomTypeById(Long roomTypeId) {

        RoomType roomType =
                roomTypeMapper.selectById(roomTypeId);

        if (roomType == null || !Integer.valueOf(1).equals(roomType.getStatus())) {
            throw new BusinessException(
                    404,
                    "房型不存在或已停售"
            );
        }

        return roomType;
    }


    /**
     * 根据入住日期、退房日期、房间数、入住人数
     * 查询真正可以预订的房型
     */
    @Override
    public List<AvailableRoomTypeVO> searchAvailableRoomTypes(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomCount,
            Integer guestCount) {

        /*
         * 1. 基础参数校验
         */
        if (checkInDate == null || checkOutDate == null) {
            throw new BusinessException(
                    400,
                    "入住日期和退房日期不能为空"
            );
        }

        if (!checkInDate.isBefore(checkOutDate)) {
            throw new BusinessException(
                    400,
                    "退房日期必须晚于入住日期"
            );
        }

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new BusinessException(
                    400,
                    "入住日期不能早于今天"
            );
        }

        if (roomCount == null || roomCount <= 0) {
            throw new BusinessException(
                    400,
                    "房间数量必须大于0"
            );
        }

        if (guestCount == null || guestCount <= 0) {
            throw new BusinessException(
                    400,
                    "入住人数必须大于0"
            );
        }


        /*
         * 2. 计算入住晚数
         *
         * 比如：
         * 10月1日入住
         * 10月4日退房
         *
         * 实际入住：
         * 10月1日
         * 10月2日
         * 10月3日
         *
         * 共3晚
         */
        long nights =
                ChronoUnit.DAYS.between(
                        checkInDate,
                        checkOutDate
                );


        /*
         * 3. 查询所有正在销售的房型
         */
        List<RoomType> roomTypes =
                roomTypeMapper.selectList(
                        new LambdaQueryWrapper<RoomType>()
                                .eq(RoomType::getStatus, 1)
                                .orderByAsc(RoomType::getSortNo)
                );


        /*
         * 最终返回给前端的结果
         */
        List<AvailableRoomTypeVO> result =
                new ArrayList<>();


        /*
         * 4. 一个房型一个房型地检查
         */
        for (RoomType roomType : roomTypes) {

            /*
             * 4.1 判断入住人数是否超过房型容量
             *
             * 例如：
             *
             * 豪华大床房：
             * maxGuests = 2
             *
             * 用户订2间：
             * 最多可住 2 × 2 = 4人
             *
             * 如果guestCount = 5
             * 这个房型直接排除
             */
            if (roomType.getMaxGuests() * roomCount < guestCount) {
                continue;
            }


            /*
             * 4.2 查询这个房型在整个入住期间的每日库存
             *
             * 日期规则：
             *
             * inventory_date >= checkInDate
             * inventory_date < checkOutDate
             *
             * 即：
             * [checkInDate, checkOutDate)
             */
            List<RoomInventory> inventories =
                    roomInventoryMapper.selectList(
                            new LambdaQueryWrapper<RoomInventory>()
                                    .eq(
                                            RoomInventory::getRoomTypeId,
                                            roomType.getRoomTypeId()
                                    )
                                    .ge(
                                            RoomInventory::getInventoryDate,
                                            checkInDate
                                    )
                                    .lt(
                                            RoomInventory::getInventoryDate,
                                            checkOutDate
                                    )
                                    .orderByAsc(
                                            RoomInventory::getInventoryDate
                                    )
                    );


            /*
             * 例如住3晚，
             * 那数据库必须正好查到3条每日库存。
             *
             * 如果只有2条，
             * 说明某一天没有初始化库存，
             * 这个房型暂时不能出售。
             */
            if (inventories.size() != nights) {
                continue;
            }


            /*
             * 4.3 计算每一天真正还能卖多少房
             *
             * available =
             * totalQty - lockedQty - soldQty
             *
             * 然后取所有入住日期里的最小值。
             *
             * 例如：
             *
             * 10月1日：10间
             * 10月2日：7间
             * 10月3日：9间
             *
             * 那连续住这3晚最多只能订7间。
             */
            int minAvailableQty =
                    inventories.stream()
                            .mapToInt(inventory ->
                                    inventory.getTotalQty()
                                            - inventory.getLockedQty()
                                            - inventory.getSoldQty()
                            )
                            .min()
                            .orElse(0);


            /*
             * 如果用户需要2间，
             * 但整个入住区间最少只剩1间，
             * 这个房型不能返回。
             */
            if (minAvailableQty < roomCount) {
                continue;
            }


            /*
             * 4.4 查询整个入住期间每天的实际售价
             */
            List<RoomPrice> prices =
                    roomPriceMapper.selectList(
                            new LambdaQueryWrapper<RoomPrice>()
                                    .eq(
                                            RoomPrice::getRoomTypeId,
                                            roomType.getRoomTypeId()
                                    )
                                    .ge(
                                            RoomPrice::getPriceDate,
                                            checkInDate
                                    )
                                    .lt(
                                            RoomPrice::getPriceDate,
                                            checkOutDate
                                    )
                                    .orderByAsc(
                                            RoomPrice::getPriceDate
                                    )
                    );


            /*
             * 住3晚就必须有3条价格。
             */
            if (prices.size() != nights) {
                continue;
            }


            /*
             * 4.5 计算一间房住完整个区间需要多少钱
             *
             * 例如：
             *
             * 10月1日 688
             * 10月2日 688
             * 10月3日 788
             *
             * pricePerRoom = 2164
             */
            BigDecimal pricePerRoom =
                    prices.stream()
                            .map(RoomPrice::getSalePrice)
                            .reduce(
                                    BigDecimal.ZERO,
                                    BigDecimal::add
                            );


            /*
             * 4.6 根据用户需要的房间数量计算订单总价
             *
             * 如果：
             *
             * 一间房3晚 = 2064
             * 用户订2间
             *
             * totalPrice = 2064 × 2
             */
            BigDecimal totalPrice =
                    pricePerRoom.multiply(
                            BigDecimal.valueOf(roomCount)
                    );


            /*
             * 4.7 组装返回给前端的VO
             */
            AvailableRoomTypeVO vo =
                    new AvailableRoomTypeVO();

            vo.setRoomTypeId(
                    roomType.getRoomTypeId()
            );

            vo.setTypeCode(
                    roomType.getTypeCode()
            );

            vo.setTypeName(
                    roomType.getTypeName()
            );

            vo.setArea(
                    roomType.getArea()
            );

            vo.setBedType(
                    roomType.getBedType()
            );

            vo.setBedCount(
                    roomType.getBedCount()
            );

            vo.setMaxGuests(
                    roomType.getMaxGuests()
            );

            vo.setAvailableQty(
                    minAvailableQty
            );

            vo.setPricePerRoom(
                    pricePerRoom
            );

            vo.setTotalPrice(
                    totalPrice
            );


            /*
             * 加入最终返回结果
             */
            result.add(vo);
        }


        /*
         * 5. 返回所有符合条件的房型
         */
        return result;
    }
}