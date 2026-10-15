package com.smarthotel.aspect;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.dto.auth.LoginRequest;
import com.smarthotel.dto.auth.LoginResponse;
import com.smarthotel.entity.OperationLogRecord;
import com.smarthotel.service.OperationLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationLogAspectTest {
    private final OperationLogService service = mock(OperationLogService.class);
    private final OperationLogAspect aspect = new OperationLogAspect(service);

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginLogUsesSubmittedUsernameAndResponseUserIdWithoutCredentials() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        LoginRequest login = new LoginRequest();
        login.setUsername("customer01");
        login.setPassword("must-never-be-logged");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[]{login});
        when(joinPoint.proceed()).thenReturn(Result.success(
                new LoginResponse("jwt-secret", 7L, "customer01", "客户", "CUSTOMER")));
        OperationLog annotation = mock(OperationLog.class);
        when(annotation.value()).thenReturn("用户登录");
        when(annotation.module()).thenReturn("AUTH");

        aspect.record(joinPoint, annotation);

        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(service).save(captor.capture());
        OperationLogRecord log = captor.getValue();
        assertEquals("customer01", log.getUsername());
        assertEquals(7L, log.getUserId());
        assertEquals("/api/v1/auth/login", log.getRequestUrl());
        assertTrue(log.getSuccess());
        assertFalse(log.toString().contains("must-never-be-logged"));
        assertFalse(log.toString().contains("jwt-secret"));
    }

    @Test
    void failedOperationIsLoggedAndOriginalErrorPropagates() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("original error"));
        OperationLog annotation = mock(OperationLog.class);
        when(annotation.value()).thenReturn("创建订单");
        when(annotation.module()).thenReturn("BOOKING");

        assertThrows(IllegalStateException.class, () -> aspect.record(joinPoint, annotation));
        ArgumentCaptor<OperationLogRecord> captor = ArgumentCaptor.forClass(OperationLogRecord.class);
        verify(service).save(captor.capture());
        assertFalse(captor.getValue().getSuccess());
    }
}
