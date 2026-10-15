package com.smarthotel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smarthotel.common.result.Result;
import com.smarthotel.entity.HotelInfo;
import com.smarthotel.entity.RoomType;
import com.smarthotel.entity.RoomTypeImage;
import com.smarthotel.mapper.HotelInfoMapper;
import com.smarthotel.mapper.RoomTypeImageMapper;
import com.smarthotel.service.RoomTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hotel")
@RequiredArgsConstructor
public class HotelPublicController {
    private final HotelInfoMapper hotelMapper;
    private final RoomTypeService roomTypeService;
    private final RoomTypeImageMapper imageMapper;

    @GetMapping
    public Result<HotelInfo> info() {
        HotelInfo hotel = hotelMapper.selectOne(new LambdaQueryWrapper<HotelInfo>()
                .eq(HotelInfo::getStatus, 1).orderByAsc(HotelInfo::getHotelId).last("LIMIT 1"));
        return Result.success(hotel);
    }

    @GetMapping("/room-types/{roomTypeId}")
    public Result<Map<String, Object>> roomType(@PathVariable Long roomTypeId) {
        RoomType roomType = roomTypeService.getRoomTypeById(roomTypeId);
        List<RoomTypeImage> images = imageMapper.selectList(new LambdaQueryWrapper<RoomTypeImage>()
                .eq(RoomTypeImage::getRoomTypeId, roomTypeId)
                .orderByDesc(RoomTypeImage::getIsCover)
                .orderByAsc(RoomTypeImage::getSortNo));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomType", roomType);
        result.put("images", images);
        return Result.success(result);
    }
}
