package com.may21.trobl.notification.dto;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.enums.UpdateType;
import com.may21.trobl.notification.domain.ContentUpdate;
import com.may21.trobl.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class NotificationDto {

    @Getter
    @AllArgsConstructor
    public static class Message {
        private String title;
        private String body;
    }


    @Getter
    public static class TokenRegistrationRequest {
        private String fcmToken;

    }

    @Getter
    public static class SendRequest {
        private final Long userId;
        private final String title;
        private final String body;
        private final ItemType itemType;
        private final Long itemId;
        private final NotificationType notificationType;
        private final Map<String, String> data;

        @Builder
        public SendRequest(Long userId, String title, String body, ItemType itemType, Long itemId,
                NotificationType notificationType, Map<String, String> data) {
            this.userId = userId;
            this.title = title;
            this.body = body;
            this.itemType = itemType;
            this.itemId = itemId;
            this.notificationType = notificationType;
            this.data = data;
        }
    }

    @Getter
    public static class ContentUpdateStatus {
        private final Long targetId;
        private final boolean unread;
        private boolean newComment;
        private boolean newLike;

        @Builder
        public ContentUpdateStatus(ContentUpdate contentUpdate) {
            this.unread = true;
            this.targetId = contentUpdate.getTargetId();
            this.newComment = contentUpdate.getChangeType() == UpdateType.COMMENT;
            this.newLike = contentUpdate.getChangeType() == UpdateType.LIKE;
        }

        public ContentUpdateStatus(Long targetId) {
            this.unread = false;
            this.targetId = targetId;
            this.newComment = false;
            this.newLike = false;

        }

        public ContentUpdateStatus update(ContentUpdate contentUpdate) {
            switch (contentUpdate.getChangeType()) {
                case COMMENT -> this.newComment = true;
                case LIKE -> this.newLike = true;
                default -> {
                    return this;
                }
            }
            return this;
        }
    }

    @Getter
    public static class Data {
        private final String title;
        private final String body;
        private final Map<String, String> data;
        private String imageUrl;
        private String sound;
        private String channelId;

        @Builder
        public Data(String title, String body, Map<String, String> data) {
            this.title = title;
            this.body = body;
            this.data = data;
        }
    }

    @Getter
    public static class BatchSendRequest {
        private List<Long> userIds;
        private String title;
        private String body;
        private Map<String, String> data;
    }

    @Getter
    @AllArgsConstructor
    public static class SubMenu {
        private final boolean myPost;
        private final boolean myComment;
        private final boolean requestedPost;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserNotiSetting {
        private boolean likeAllowed;
        private boolean commentAllowed;
        private boolean marketingAllowed;

        public UserNotiSetting(User user) {
            this.likeAllowed = user.canReceiveNotification(NotificationType.LIKE);
            this.commentAllowed = user.canReceiveNotification(NotificationType.COMMENT);
            this.marketingAllowed = user.canReceiveNotification(NotificationType.MARKETING);
        }
    }
}
