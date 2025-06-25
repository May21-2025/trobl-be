package com.may21.trobl._global.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

    private String description;
    private String message;
    private int status;
    private String code;

    public ErrorResponse(ExceptionCode errorCode) {
        this.message = errorCode.getMessage();
        this.status = errorCode.getStatus().value();
        this.code = errorCode.getCode();
        this.description = null;
    }

    public ErrorResponse(ExceptionCode errorCode, Exception e) {
        this.message = errorCode.getMessage();
        this.status = errorCode.getStatus().value();
        this.code = errorCode.getCode();
        this.description = e.getMessage();
    }
}
