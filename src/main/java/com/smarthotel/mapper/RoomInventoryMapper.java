package com.smarthotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthotel.entity.RoomInventory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RoomInventoryMapper
        extends BaseMapper<RoomInventory> {
}