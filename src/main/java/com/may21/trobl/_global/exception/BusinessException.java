package com.may21.trobl._global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

  private final ExceptionCode errorCode;

  public BusinessException(String message, ExceptionCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(ExceptionCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
