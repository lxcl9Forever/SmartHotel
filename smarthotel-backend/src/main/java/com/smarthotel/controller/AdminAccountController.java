package com.smarthotel.controller;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.dto.admin.CreateEmployeeRequest;
import com.smarthotel.dto.admin.UpdateEmployeeRequest;
import com.smarthotel.service.AdminAccountService;
import com.smarthotel.vo.AdminEmployeeVO;
import com.smarthotel.vo.AdminPaymentVO;
import com.smarthotel.vo.AdminUserVO;
import com.smarthotel.vo.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAccountController {
    private final AdminAccountService service;

    @GetMapping("/users")
    public Result<PageResult<AdminUserVO>> users(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(service.users(page, pageSize));
    }

    @PostMapping("/users/{userId}/status")
    @OperationLog(value = "修改用户状态", module = "USER")
    public Result<Void> userStatus(@PathVariable Long userId, @RequestParam String status,
                                   Authentication authentication) {
        service.setUserStatus(userId, status, (Long) authentication.getCredentials());
        return Result.success();
    }

    @GetMapping("/employees")
    public Result<PageResult<AdminEmployeeVO>> employees(@RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(service.employees(page, pageSize));
    }

    @PostMapping("/employees")
    @OperationLog(value = "创建员工", module = "EMPLOYEE")
    public Result<Void> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        service.createEmployee(request);
        return Result.success();
    }

    @PutMapping("/employees/{employeeId}")
    @OperationLog(value = "修改员工", module = "EMPLOYEE")
    public Result<Void> updateEmployee(@PathVariable Long employeeId,
                                       @Valid @RequestBody UpdateEmployeeRequest request) {
        service.updateEmployee(employeeId, request);
        return Result.success();
    }

    @GetMapping("/payments")
    public Result<PageResult<AdminPaymentVO>> payments(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(service.payments(page, pageSize));
    }
}
