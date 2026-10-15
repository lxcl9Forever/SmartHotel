package com.smarthotel.vo;

public record AdminUserVO(Long userId, String username, String nickname, String phone,
                          String role, String status) {}
