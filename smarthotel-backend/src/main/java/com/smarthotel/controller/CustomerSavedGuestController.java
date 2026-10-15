package com.smarthotel.controller;

import com.smarthotel.common.result.Result;
import com.smarthotel.dto.auth.SavedGuestRequest;
import com.smarthotel.entity.CustomerSavedGuest;
import com.smarthotel.service.CustomerSavedGuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/saved-guests")
@RequiredArgsConstructor
public class CustomerSavedGuestController {
    private final CustomerSavedGuestService service;

    @GetMapping
    public Result<List<CustomerSavedGuest>> list(Authentication authentication) {
        return Result.success(service.list((Long) authentication.getCredentials()));
    }

    @PostMapping
    public Result<CustomerSavedGuest> create(Authentication authentication,
                                            @Valid @RequestBody SavedGuestRequest request) {
        return Result.success(service.create((Long) authentication.getCredentials(), request));
    }

    @PutMapping("/{guestId}")
    public Result<CustomerSavedGuest> update(Authentication authentication, @PathVariable Long guestId,
                                            @Valid @RequestBody SavedGuestRequest request) {
        return Result.success(service.update((Long) authentication.getCredentials(), guestId, request));
    }

    @DeleteMapping("/{guestId}")
    public Result<Void> delete(Authentication authentication, @PathVariable Long guestId) {
        service.delete((Long) authentication.getCredentials(), guestId);
        return Result.success();
    }
}
