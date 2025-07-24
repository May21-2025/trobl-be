package com.may21.trobl._global.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


@Getter
@Slf4j
public class BusinessException extends RuntimeException {

    private final ExceptionCode errorCode;
    private final String detailMessage;

    public BusinessException(ExceptionCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = "";
    }

    public BusinessException(ExceptionCode errorCode, Exception e) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = e.getMessage();
        log.error("BusinessException occurred: {}", e.getMessage());
    }

    public BusinessException(ExceptionCode errorCode, String message) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detailMessage = message;
        log.error("BusinessException occurred: {}", message);
    }
}
