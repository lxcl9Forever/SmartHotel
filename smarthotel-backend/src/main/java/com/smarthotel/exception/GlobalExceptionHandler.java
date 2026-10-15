package com.smarthotel.exception;

import com.smarthotel.common.result.Result;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicate(DuplicateKeyException e) {
        return ResponseEntity.status(409).body(Result.error(409, "记录已存在，请检查账号、手机号或工号"));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            BusinessException e) {

        return ResponseEntity
                .status(e.getCode())
                .body(Result.error(
                        e.getCode(),
                        e.getMessage()
                ));
    }
}
