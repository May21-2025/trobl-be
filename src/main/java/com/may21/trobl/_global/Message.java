package com.may21.trobl._global;

import com.may21.trobl._global.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
public class Message {
    private boolean success;
    private Object data;
    private Error error;

    public static Message success(Object data) {
        return new Message(true, data, null);
    }

    public static Message fail(Object data, ExceptionCode code) {
        return new Message(false, data, new Error(code.getCode(), code.getMessage()));
    }

    public static Message fail(ExceptionCode code) {
        return new Message(false, null, new Error(code.getCode(), code.getMessage()));
    }

    @Getter
    @AllArgsConstructor
    static class Error {
        private String code;
        private String message;
    }
}
