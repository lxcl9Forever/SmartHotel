package com.smarthotel.aspect;

import com.smarthotel.annotation.OperationLog;
import com.smarthotel.common.result.Result;
import com.smarthotel.dto.auth.LoginRequest;
import com.smarthotel.dto.auth.LoginResponse;
import com.smarthotel.entity.OperationLogRecord;
import com.smarthotel.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private final OperationLogService service;

    @Around("@annotation(annotation)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog annotation) throws Throwable {
        boolean success = false;
        Object response = null;
        try {
            response = joinPoint.proceed();
            success = (!(response instanceof Result<?> result) || Integer.valueOf(200).equals(result.getCode()))
                    && !"failure".equals(response);
            return response;
        } finally {
            try {
                OperationLogRecord record = new OperationLogRecord();
                record.setOperation(annotation.value());
                record.setModule(annotation.module());
                record.setSuccess(success);
                LoginRequest loginRequest = null;
                for (Object argument : joinPoint.getArgs()) {
                    if (argument instanceof LoginRequest login) {
                        loginRequest = login;
                        break;
                    }
                }
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (loginRequest != null) {
                    record.setUsername(loginRequest.getUsername());
                    if (response instanceof Result<?> result && result.getData() instanceof LoginResponse login) {
                        record.setUserId(login.getUserId());
                    }
                } else if (authentication != null && authentication.isAuthenticated()
                        && !"anonymousUser".equals(authentication.getPrincipal())) {
                    record.setUsername(authentication.getName());
                    if (authentication.getCredentials() instanceof Long id) record.setUserId(id);
                }
                if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                    HttpServletRequest request = attributes.getRequest();
                    record.setRequestUrl(request.getRequestURI());
                    record.setIp(request.getRemoteAddr());
                } else {
                    record.setRequestUrl("unknown");
                }
                service.save(record);
            } catch (RuntimeException error) {
                log.error("Could not save operation log for {}", annotation.value(), error);
            }
        }
    }
}
