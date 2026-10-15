package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @GetMapping("/me")
    public Result<Map<String, Object>> me(
            Authentication authentication) {

        Map<String, Object> data = new HashMap<>();

        data.put(
                "userId",
                authentication.getCredentials()
        );

        data.put(
                "username",
                authentication.getName()
        );

        data.put(
                "authorities",
                authentication.getAuthorities()
        );

        return Result.success(data);
    }
}
