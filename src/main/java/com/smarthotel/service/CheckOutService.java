package com.smarthotel.service;

import com.smarthotel.dto.checkout.CheckOutRequest;

public interface CheckOutService {

    void checkOut(
            String username,
            CheckOutRequest request
    );
}