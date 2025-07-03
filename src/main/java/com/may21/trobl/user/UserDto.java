package com.may21.trobl.user;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class UserDto {

    @Getter
    @AllArgsConstructor
    public static class Info {
        private final Long userId;
        private final String nickname;
        private final String thumbnailUrl;

        public Info(User user) {
            this.userId = user.getId();
            this.nickname = user.getNickname();
            this.thumbnailUrl = user.getThumbnailKey() != null ?
                    GlobalValues.getCdnUrl()+user.getThumbnailKey() : null;
        }
    }

    @Getter
    public static class InfoDetail extends Info {
        private final String email;
        private final String address;
        private final boolean married;
        private final LocalDate marriedDate;
        private final LocalDate signedUpAt;

        public InfoDetail(User user) {
            super(user);
            this.email = user.getEmail();
            this.address = user.getAddress();
            this.married = user.isMarried();
            this.marriedDate = user.getWeddingAnniversaryDate();
            this.signedUpAt = user.getSignUpDate();
        }
    }

    @Getter
    public class AlertSetting {
        private final boolean emailNotification;
        private final boolean pushNotification;

        public AlertSetting(boolean emailNotification, boolean pushNotification) {
            this.emailNotification = emailNotification;
            this.pushNotification = pushNotification;
        }
    }

    @Getter
    public class InfoRequest {
        private final LocalDate marriageDate;
        private final String partnerEmail;

        public InfoRequest(LocalDate marriageDate, String partnerEmail) {
            this.marriageDate = marriageDate;
            this.partnerEmail = partnerEmail;
        }
    }

    @Getter
    @AllArgsConstructor
    public class NotificationSetting {
        private final boolean post;
        private final boolean comment;
        private final boolean like;
        private final boolean view;
    }

    @Getter
    @AllArgsConstructor
    public class Request {
        private final String nickname;
        private final String address;
        private final Boolean married;
        private final LocalDate weddingAnniversaryDate;
        @Setter  private String imageKey;
    }
}
