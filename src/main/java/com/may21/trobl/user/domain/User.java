package com.may21.trobl.user.domain;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.enums.Language;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.enums.OAuthProvider;
import com.may21.trobl._global.enums.RoleType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.utility.Utility;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.pushAlarm.DeviceFcmToken;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.user.UserDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.may21.trobl._global.component.GlobalValues.USER_PROFILE_IMAGE_PATH;
import static com.may21.trobl._global.enums.NotificationType.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(name = "app_users")
public class User implements UserDetails, OAuth2User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;
    private String nickname;
    private LocalDate nicknameUpdatedAt;
    private String address;

    private Language language;

    private boolean married;
    @Setter
    private Long partnerId;
    @Setter
    private LocalDate weddingAnniversaryDate;

    private boolean unregistered = false;
    private boolean testUser = false;
    private Boolean reported = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeviceFcmToken> fcmTokens;

    private String thumbnailKey;
    @Setter
    private LocalDateTime thumbnailUpdatedAt;

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_notification_setting", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "notification_type")
    private List<NotificationType> blockedNotificationTypes;

    private int failedLoginAttempts;

    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;

    private LocalDate lastLoginDate;

    @CreatedDate
    private LocalDate signUpDate;


    public User(Long userId, String subject, String s, List<String> roles) {
        this.id = userId;
        this.username = subject;
        this.password = s;
        this.roles = roles;
    }

    @Builder
    public User(String username, String encryptPassword, String nickname, String provider,
            RoleType role, Boolean isTestUser, String address) {
        this.username = username;
        this.provider = OAuthProvider.fromString(provider);
        this.password = encryptPassword == null ? "oauth" : encryptPassword;
        this.nickname = nickname;
        this.roles = List.of(role.name());
        this.testUser = isTestUser != null && isTestUser;
        this.address = address;
        // add default values for blocked notifications
        this.blockedNotificationTypes = new ArrayList<>();
        this.blockedNotificationTypes.add(MARKETING);
    }

    public User(Long userId, String subject, String s, String role) {
        List<String> roles = new ArrayList<>();
        roles.add(role);
        this.id = userId;
        this.username = subject;
        this.password = s;
        this.roles = roles;
    }

    public User(UserDto.Info info) {
        this.id = info.getUserId();
        this.username = info.getUsername();
        this.nickname = info.getNickname();
    }

    public User(RedisDto.UserDto userDto) {
        this.id = userDto.getUserId();
        this.username = userDto.getUsername();
        this.nickname = userDto.getNickname();
    }

    public String getThumbnailUrl() {
        String ts = thumbnailUpdatedAt == null ? "" : "?ts=" +thumbnailUpdatedAt ;
        return thumbnailKey == null ? null :
                GlobalValues.getCdnUrl() + GlobalValues.getPREFIX() + USER_PROFILE_IMAGE_PATH +
                        thumbnailKey+ts;
    }

    public void setPartner(User partner, UserDto.AcceptPartnerRequest request) {
        this.partnerId = partner.getId();
        this.married = true;
        this.weddingAnniversaryDate = request.marriageDate();
    }

    public void setPartner(User partner, AdminDto.ConnectPartners request) {
        this.partnerId = partner.getId();
        this.married = true;
        this.weddingAnniversaryDate = request.getMarriageDate();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of("id", id, "username", username, "nickname", nickname);
    }

    @Override
    public String getName() {
        return this.username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void updatePassword(String encodePassword) {
        this.password = encodePassword;
    }

    public void updateNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            throw new BusinessException(ExceptionCode.NICKNAME_CANNOT_BE_BLANK);
        }
        this.nickname = nickname;
        this.nicknameUpdatedAt = LocalDate.now();
    }

    public void updateInformation(UserDto.MarriedInfo requestBody) {
        if (requestBody.getMarriageDate() != null) {
            this.weddingAnniversaryDate = requestBody.getMarriageDate();
            this.married = true;
        }
    }

    public void setNotification(NotificationType type, Boolean enabled) {
    }

    public OAuthProvider getOauthProvider() {
        if (provider == null) return OAuthProvider.NONE;
        return provider;
    }

    public void updateAddress(String address) {
        if (address == null || address.isEmpty()) {
            return;
        }
        this.address = address;
    }

    public void addRole(RoleType role) {
        if (this.roles == null) {
            this.roles = new java.util.ArrayList<>();
        }
        String roleName = role.name();
        if (!this.roles.contains(roleName)) { // 중복 추가 방지
            this.roles.add(roleName);
        }
    }

    public void removeRole(RoleType role) {
        if (this.roles != null) {
            this.roles.remove(role.name());
        }
    }

    public void setRoles(List<RoleType> newRoleTypes) {
        List<String> newRoles = new ArrayList<>();
        for (RoleType roleType : newRoleTypes) {
            if (roleType != null) {
                newRoles.add(roleType.name());
            }
        }
        this.roles = newRoles;
    }

    public String getRole() {
        if (roles == null || roles.isEmpty()) {
            return RoleType.USER.name(); // 기본 역할을 USER로 설정
        }
        return roles.getFirst(); // 첫 번째 역할 반환
    }

    public List<String> getFcmTokenList() {
        if (fcmTokens == null || fcmTokens.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> tokens = new ArrayList<>();
        for (DeviceFcmToken token : fcmTokens) {
            tokens.add(token.getFcmToken());
        }
        return tokens;
    }

    public LocalDate getNicknameUpdatedAt() {
        // 값이 null인 경우 현재에서 한달 전 날짜를 반환
        return nicknameUpdatedAt != null ? nicknameUpdatedAt : LocalDate.now()
                .minusMonths(1);
    }

    public void setUnregistered(int unregisteredUserCount) {
        this.unregistered = true;
        this.nickname = Utility.getRandomString();
        this.username = "unregistered_" + unregisteredUserCount;
        this.password = "unregistered";
        this.thumbnailKey = null;
        this.address = null;
        this.married = false;
        this.partnerId = null;
        this.weddingAnniversaryDate = null;
        this.language = Language.KOR;
        this.fcmTokens = new ArrayList<>();
        this.roles = List.of(RoleType.NONE.name());
        this.failedLoginAttempts = 0;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = false;
        this.signUpDate = LocalDate.now();
        this.lastLoginDate = null;
        this.blockedNotificationTypes = List.of();
        this.nicknameUpdatedAt = LocalDate.now();
        this.reported = false;
        this.provider = OAuthProvider.NONE;
        this.testUser = false;
    }

    public void setLastLoginDate() {
        if (this.lastLoginDate == null || this.lastLoginDate.isBefore(LocalDate.now())) {
            this.lastLoginDate = LocalDate.now();
        }
    }

    /**
     * 특정 알림 타입이 차단되어 있는지 확인
     *
     * @param notificationType 확인할 알림 타입
     * @return 차단되어 있으면 true, 허용되어 있으면 false
     */
    public boolean isNotificationBlocked(NotificationType notificationType) {
        if (blockedNotificationTypes == null) {
            return false;
        }
        return blockedNotificationTypes.contains(notificationType);
    }

    /**
     * 특정 알림 타입을 차단
     *
     * @param notificationType 차단할 알림 타입 (COMMENT, LIKE, MARKETING만 가능)
     */
    public void blockNotification(NotificationType notificationType) {
        if (!isBlockableNotificationType(notificationType)) {
            throw new BusinessException(ExceptionCode.NOTIFICATION_TYPE_NOT_BLOCKABLE);
        }

        if (blockedNotificationTypes == null) {
            blockedNotificationTypes = new ArrayList<>();
        }

        if (!blockedNotificationTypes.contains(notificationType)) {
            blockedNotificationTypes.add(notificationType);
        }
    }

    /**
     * 특정 알림 타입 차단 해제
     *
     * @param notificationType 차단 해제할 알림 타입
     */
    public void unblockNotification(NotificationType notificationType) {
        if (blockedNotificationTypes != null) {
            blockedNotificationTypes.remove(notificationType);
        }
    }

    /**
     * 알림 타입의 차단 상태를 토글 (차단 <-> 허용)
     *
     * @param notificationType 토글할 알림 타입
     */
    public void toggleNotificationBlock(NotificationType notificationType) {
        if (isNotificationBlocked(notificationType)) {
            unblockNotification(notificationType);
        }
        else {
            blockNotification(notificationType);
        }
    }

    /**
     * 차단 가능한 알림 타입인지 확인
     *
     * @param notificationType 확인할 알림 타입
     * @return 차단 가능하면 true
     */
    private boolean isBlockableNotificationType(NotificationType notificationType) {
        return notificationType == COMMENT || notificationType == LIKE ||
                notificationType == NotificationType.MARKETING;
    }

    /**
     * 알림을 받을 수 있는 상태인지 확인 (특정 타입)
     *
     * @param notificationType 확인할 알림 타입
     * @return 알림을 받을 수 있으면 true
     */
    public boolean canReceiveNotification(NotificationType notificationType) {
        // 계정이 활성화되어 있고, 해당 알림이 차단되지 않은 경우
        return this.enabled && !isNotificationBlocked(notificationType);
    }

    public void setThumbnailKey(String thumbnailKey) {
        this.thumbnailKey = thumbnailKey;
        this.thumbnailUpdatedAt = LocalDateTime.now();
    }

}
