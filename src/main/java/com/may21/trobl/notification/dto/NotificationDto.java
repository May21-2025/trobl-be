package com.may21.trobl.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

public class NotificationDto {

    @Getter
    public static class TokenRegistrationRequest {
        private String deviceToken;
    }

    @Getter
    public static class SendRequest {
        private Long userId;
        private String title;
        private String body;
        private Map<String, String> data;

        @Builder
        public SendRequest(Long userId, String title, String body, Map<String, String> data) {
            this.userId = userId;
            this.title = title;
            this.body = body;
            this.data = data;
        }
    }
}
