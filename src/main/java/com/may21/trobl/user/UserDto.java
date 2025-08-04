package com.may21.trobl.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.may21.trobl._global.enums.RequestStatus;
import com.may21.trobl.partner.PartnerRequest;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class UserDto {

    @Getter
    public static class Info {
        private final Long userId;
        private final String username;
        private final String nickname;
        @Setter
        private String thumbnailUrl;

        @JsonCreator
        public Info(@JsonProperty("userId") Long userId, @JsonProperty("username") String username,
                @JsonProperty("nickname") String nickname,
                @JsonProperty("thumbnailUrl") String thumbnailUrl) {
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.thumbnailUrl = thumbnailUrl;
        }

        public Info(User user) {
            this.userId = user.getId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.thumbnailUrl = user.getThumbnailUrl();
        }

        public Info(RedisDto.UserDto user) {

            this.userId = user.getUserId();
            this.username = user.getUsername();
            this.nickname = user.getNickname();
            this.thumbnailUrl = user.getThumbnailUrl();
        }
    }

    @Getter
    public static class Update {
        private final String nickname;
        private final String address;
        private final Boolean married;
        private final LocalDate marriageDate;

        @JsonCreator
        public Update(@JsonProperty("nickname") String nickname,
                @JsonProperty("address") String address, @JsonProperty("married") Boolean married,
                @JsonProperty("marriageDate") LocalDate marriageDate) {
            this.nickname = nickname;
            this.address = address;
            this.married = married;
            this.marriageDate = marriageDate;
        }
    }

    @Getter
    public static class InfoDetail extends Info {
        private final String username;
        private final String OAuthType;
        private final boolean married;
        private final String address;
        private final UserDto.Info partnerInfo;
        private final LocalDate marriageDate;
        private final LocalDate signedUpAt;
        private final List<String> roles;

        public InfoDetail(User user, User partner) {
            super(user);
            this.username = user.getUsername();
            this.OAuthType = user.getProvider() != null ? user.getProvider()
                    .name() : null;
            this.married = user.isMarried();
            this.address = user.getAddress();
            this.marriageDate = user.getWeddingAnniversaryDate();
            this.partnerInfo = partner != null ? new UserDto.Info(partner) : null;
            this.signedUpAt = user.getSignUpDate();
            this.roles = user.getRoles();
        }

        public Collection<GrantedAuthority> getAuthorities() {
            //make roles to GrantedAuthority
            return roles.stream()
                    .map(role -> (GrantedAuthority) () -> "ROLE_" + role)
                    .toList();
        }


    }

    @Getter
    public static class AlertSetting {
        private final boolean emailNotification;
        private final boolean pushNotification;

        public AlertSetting(boolean emailNotification, boolean pushNotification) {
            this.emailNotification = emailNotification;
            this.pushNotification = pushNotification;
        }
    }

    @Getter
    public static class MarriedInfo {
        private final LocalDate marriageDate;
        private final String partnerEmail;

        public MarriedInfo(LocalDate marriageDate,  String partnerEmail) {
            this.marriageDate = marriageDate;
            this.partnerEmail = partnerEmail;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class NotificationSetting {
        private final boolean post;
        private final boolean comment;
        private final boolean like;
        private final boolean view;
    }

    @Getter
    @AllArgsConstructor
    public static class PartnerInfo {
        private final String partnerUserName;
        private final String partnerNickname;
        private final RequestStatus status;
        private final Long userId;
        private final Long partnerId;
        private final Long partnerRequestId;

        public PartnerInfo(User user, User partner, PartnerRequest partnerRequest) {
            this.partnerUserName = partner.getUsername();
            this.partnerNickname = partner.getNickname();
            this.status = partnerRequest.getStatus();
            this.userId = user.getId();
            this.partnerId = partner.getId();
            this.partnerRequestId = partnerRequest.getId();
        }
    }

    public record RequestPartner(String username, LocalDate marriageDate) {}

    public record AcceptPartnerRequest(boolean accepted, LocalDate marriageDate) {}
}
