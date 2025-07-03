package com.may21.trobl.user.domain;

import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.enums.Language;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.enums.OAuthProvider;
import com.may21.trobl._global.enums.RoleType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.notification.domain.NotificationSetting;
import com.may21.trobl.storage.GoogleCloudStorageService;
import com.may21.trobl.user.UserDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.may21.trobl._global.component.GlobalValues.USER_PROFILE_IMAGE_PATH;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EntityListeners(AuditingEntityListener.class)
@Table(name = "app_users")
public class User implements UserDetails, OAuth2User  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String nickname;
    private String email;

    private boolean married;

    private Language language;

    private LocalDate weddingAnniversaryDate;

    private LocalDate nicknameUpdatedAt;

    private Long partnerId;

    private String address;

    private String thumbnailKey;

    private OAuthProvider oauthProvider;

    @OneToOne(cascade = CascadeType.ALL)
    private NotificationSetting setting;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private List<String> roles;

    private String provider;
    private int failedLoginAttempts;

    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;

    @CreatedDate
    private LocalDate signUpDate;


    public User(Long userId, String subject, String s, Collection<GrantedAuthority> authorities) {
        this.id = userId;
        this.username = subject;
        this.password = s;
        this.roles =
                authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(role -> role.replace("ROLE_", ""))
                        .toList();
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
        this.enabled = true;
    }

    @Builder
    public User(String username, String encryptPassword, String encryptEmail, String nickname, String provider, RoleType role) {
        this.username = username;
        this.email = encryptEmail;
        this.provider = provider == null ? "NONE" : provider;
        this.password = encryptPassword == null ? "oauth" : encryptPassword;
        this.nickname = nickname;
        this.roles = List.of(role.name());
    }

    public void update(UserDto.Request request) {
        String newNickname = request.getNickname();
        String newAddress = request.getAddress();
        LocalDate newWeddingAnniversaryDate = request.getWeddingAnniversaryDate();
        Boolean newMarried = request.getMarried();
        String imageKey = request.getImageKey();
        if (newNickname != null && !newNickname.isEmpty()) {
            this.updateNickname(newNickname);
        }
        if (newAddress != null && !newAddress.isEmpty()) {
            this.address = newAddress;
        }
        if (newWeddingAnniversaryDate != null) {
            this.weddingAnniversaryDate = newWeddingAnniversaryDate;
            this.married = newMarried != null ? newMarried : false;
        }
        if (imageKey != null) {
            this.thumbnailKey = imageKey.isEmpty() ? null : imageKey;
        }
    }

    public String getThumbnailUrl() {
        return thumbnailKey == null ? null : GlobalValues.getCdnUrl() + USER_PROFILE_IMAGE_PATH + thumbnailKey;
    }

    public void setPartner(User partner) {
        this.partnerId = partner.getId();
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
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
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
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        this.nickname = nickname;
        this.nicknameUpdatedAt = LocalDate.now();
    }

    public void updateInformation(UserDto.InfoRequest requestBody) {
        if (requestBody.getMarriageDate() != null) {
            this.weddingAnniversaryDate = requestBody.getMarriageDate();
            this.married = true;
        }
    }

    public void setNotification(NotificationType type, Boolean enabled) {
    }

    public OAuthProvider getOauthProvider() {
        if (oauthProvider == null) return OAuthProvider.NONE;
        return oauthProvider;
    }
}
