package com.smarthotel.service;

public interface RoomStatusService {

    void startCleaning(
            String username,
            Long roomId
    );

    void finishCleaning(
            String username,
            Long roomId
    );

    void startMaintenance(
            String username,
            Long roomId,
            String reason
    );

    void setOutOfService(
            String username,
            Long roomId,
            String reason
    );

    void restoreRoom(
            String username,
            Long roomId,
            String reason
    );
}