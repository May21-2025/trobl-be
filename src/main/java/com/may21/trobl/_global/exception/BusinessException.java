package com.may21.trobl._global.exception;

import lombok.Getter;

@Getter
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
  }

  public BusinessException(ExceptionCode exceptionCode, String postingAlreadyHasAPoll) {
    super(exceptionCode.getMessage());
    this.errorCode = exceptionCode;
    this.detailMessage = postingAlreadyHasAPoll;
  }
}
