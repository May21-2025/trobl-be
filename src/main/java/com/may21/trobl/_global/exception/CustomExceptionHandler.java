package com.may21.trobl._global.exception;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.exception.domain.ExceptionLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {
  private final ExceptionHandleService errorLogService;

  public CustomExceptionHandler(
      ExceptionHandleService errorLogService) {
    this.errorLogService = errorLogService;
  }

  private static void logError(ExceptionCode code, ExceptionLog errorLog, StackTraceElement stack) {
    String number =
        errorLog.getCodeLineNumber() == -1 ? "" : errorLog.getCodeLineNumber().toString();
    log.error(
        """

                ************************************
                    Error Details
                  - Code       : {}
                  - Message    : {}
                  - From       : {} {} {}
                  - Path       : {}
                ************************************
                """,
        code.getCode(),
        errorLog.getMessage(),
        errorLog.getServiceName(),
        errorLog.getMethodName(),
        number,
        stack == null ? "" : stack);
  }

  @ExceptionHandler(BusinessException.class)
  protected ResponseEntity<Message> handleBusinessException(final BusinessException e) {
    final ExceptionCode errorCode = e.getErrorCode();
    ExceptionLog errorLog = ExceptionHandleService.getExceptionLog(e);
    StackTraceElement stack = e.getStackTrace()[0];
    logError(errorCode, errorLog, stack);
    return new ResponseEntity<>(Message.fail(errorCode), errorCode.getStatus());
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<Message> handleGlobalException(final Exception e) {
    ExceptionLog errorLog = errorLogService.saveErrorLog(e);
    ExceptionCode code = ExceptionCode.INTERNAL_SERVER_ERROR;
    StackTraceElement stack = e.getStackTrace()[0];
    logError(code, errorLog, stack);

    return new ResponseEntity<>(Message.fail(code), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
