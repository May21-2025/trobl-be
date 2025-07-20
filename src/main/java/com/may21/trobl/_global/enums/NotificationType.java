package com.may21.trobl._global.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    COMMENT(NotificationStrategy.BATCHED, "notification.comment"),
    LIKE(NotificationStrategy.BATCHED, "notification.like"),
    VOTE(NotificationStrategy.BATCHED, "notification.vote"),
    CONTENT_RECOMMENDATION(NotificationStrategy.SCHEDULED, "notification.content_recommendation"),
    POPULAR_POST(NotificationStrategy.SCHEDULED, "notification.popular_post"),
    FAIRVIEW_REQUEST(NotificationStrategy.IMMEDIATE, "notification.fairview_request"),
    QUICK_POLL_PARTICIPATION(NotificationStrategy.BATCHED, "notification.quick_poll_participation"),
    COMMUNITY_ANALYSIS(NotificationStrategy.SCHEDULED, "notification.community_analysis"),
    ANNOUNCEMENT(NotificationStrategy.SCHEDULED, "notification.announcement"),
    POST_DELETED(NotificationStrategy.SCHEDULED, "notification.post_deleted"),
    COMMENT_DELETED(NotificationStrategy.SCHEDULED, "notification.comment_deleted"),
    MARKETING(NotificationStrategy.SCHEDULED, "notification.marketing"),
    ETC(NotificationStrategy.SCHEDULED, "notification.etc");


    private final NotificationStrategy defaultStrategy;
    private final String messageKey;

    NotificationType(NotificationStrategy defaultStrategy, String messageKey) {
        this.defaultStrategy = defaultStrategy;
        this.messageKey = messageKey;
    }

}

