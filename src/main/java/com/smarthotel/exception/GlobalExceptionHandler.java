package com.smarthotel.exception;

import com.smarthotel.common.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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