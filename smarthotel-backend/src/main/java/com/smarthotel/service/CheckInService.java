package com.smarthotel.service;

import com.smarthotel.dto.checkin.CheckInRequest;
import com.smarthotel.vo.AvailablePhysicalRoomVO;

import java.util.List;

public interface CheckInService {

    List<AvailablePhysicalRoomVO>
    listAvailableRooms(
            Long orderId
    );

    void checkIn(
            String username,
            CheckInRequest request
    );
}