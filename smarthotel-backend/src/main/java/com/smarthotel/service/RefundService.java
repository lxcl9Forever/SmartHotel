package com.smarthotel.service;

public interface RefundService {


    void applyRefund(
            Long orderId,
            Long userId,
            String reason
    );


}