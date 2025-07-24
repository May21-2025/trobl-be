package com.may21.trobl.admin;

import lombok.Getter;

import java.util.Map;


public class AdminDto {
    @Getter
    public static class PushNotification {
        private Long userId;
        private String title;
        private String message;
        private Map<String, String> data;
    }
}
