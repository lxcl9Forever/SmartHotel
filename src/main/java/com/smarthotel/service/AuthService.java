package com.smarthotel.service;

import com.smarthotel.dto.auth.LoginRequest;
import com.smarthotel.dto.auth.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}