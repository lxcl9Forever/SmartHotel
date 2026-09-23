package com.smarthotel.service;

import com.smarthotel.entity.RoomType;
import com.smarthotel.vo.AvailableRoomTypeVO;

import java.time.LocalDate;
import java.util.List;

public interface RoomTypeService {

    List<RoomType> listAvailableRoomTypes();

    RoomType getRoomTypeById(Long roomTypeId);

    List<AvailableRoomTypeVO> searchAvailableRoomTypes(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer roomCount,
            Integer guestCount
    );
}