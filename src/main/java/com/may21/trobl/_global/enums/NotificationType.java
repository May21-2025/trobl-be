package com.may21.trobl._global.enums;

import com.may21.trobl.notification.service.NotificationServiceImpl;

public enum NotificationType {
        COMMENT(NotificationStrategy.BATCHED, "notification.comment"),
        LIKE(NotificationStrategy.BATCHED, "notification.like"),
        VOTE(NotificationStrategy.BATCHED, "notification.vote"),
        CONTENT_RECOMMENDATION(NotificationStrategy.SCHEDULED, "notification.content_recommendation"),
        POPULAR_POST(NotificationStrategy.SCHEDULED, "notification.popular_post"),
        FAIRVIEW_REQUEST(NotificationStrategy.IMMEDIATE, "notification.fairview_request"),
        QUICKPOLL_PARTICIPATION(NotificationStrategy.BATCHED, "notification.quickpoll_participation"),
        COMMUNITY_ANALYSIS(NotificationStrategy.SCHEDULED, "notification.community_analysis"),
        ANNOUNCEMENT(NotificationStrategy.SCHEDULED, "notification.announcement"),
    EIGHT(NotificationStrategy.SCHEDULED, "notification.eight"),
    NINE(NotificationStrategy.SCHEDULED, "notification.nine"),
    TEN(NotificationStrategy.SCHEDULED, "notification.ten"),
    ETC(NotificationStrategy.SCHEDULED, "notification.etc");


    private final NotificationStrategy defaultStrategy;
    private final String messageKey;

    NotificationType(NotificationStrategy defaultStrategy, String messageKey) {
        this.defaultStrategy = defaultStrategy;
        this.messageKey = messageKey;
    }

    public NotificationStrategy getDefaultStrategy() {
        return defaultStrategy;
    }

    public String getMessageKey() {
        return messageKey;
    }

}

