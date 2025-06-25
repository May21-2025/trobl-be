package com.may21.trobl._global.exception;

import com.may21.trobl._global.exception.domain.ExceptionLog;
import com.may21.trobl._global.exception.domain.ExceptionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionHandleService {

    public static final String PROJECT_PACKAGE_PATH = "com.may21.trobl";
    private final ExceptionLogRepository exceptionLogRepository;

    public static ExceptionLog getExceptionLog(Exception e) {
        String message = e.getMessage();
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        StackTraceElement[] mainTrace =
                Arrays.stream(stackTraceElements)
                        .filter(stack -> stack.getClassName().contains(PROJECT_PACKAGE_PATH))
                        .toArray(StackTraceElement[]::new);
        // if mainTrace is empty then return first element of stackTraceElements
        StackTraceElement main = mainTrace.length == 0 ? stackTraceElements[0] : mainTrace[0];
        String methodName = main.getMethodName();
        String className = main.getClassName().replace(PROJECT_PACKAGE_PATH, "");
        String fileName = main.getFileName();
        Integer codeLineNumber = main.getLineNumber();
        String stackTrace =
                mainTrace.length == 0
                        ? Arrays.stream(e.getStackTrace())
                        .map(stack -> stack.getClassName() + "." + stack.getMethodName())
                        .collect(Collectors.joining("\n"))
                        : Arrays.stream(mainTrace)
                        .map(
                                stack ->
                                        stack.getClassName().replace(PROJECT_PACKAGE_PATH, "")
                                                + "."
                                                + stack.getMethodName()
                                                + "."
                                                + stack.getLineNumber())
                        .collect(Collectors.joining("\n"));
        return new ExceptionLog(message, methodName, className, fileName, codeLineNumber, stackTrace);
    }

    public ExceptionLog saveErrorLog(Exception e) {
        ExceptionLog errorLog = getExceptionLog(e);
        return exceptionLogRepository.save(errorLog);
    }

    public boolean isFromCode(Exception e) {
        return Arrays.stream(e.getStackTrace())
                .anyMatch(stack -> stack.getClassName().contains(PROJECT_PACKAGE_PATH));
    }
}
