package com.may21.trobl.user.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@Entity
@Getter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "text")
    private String tokenId;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String token;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(nullable = false, columnDefinition = "text")
    private String deviceId;

    @Column(nullable = false)
    private String ipAddress;

    private String parentId;

    @Setter
    @Column(nullable = false)
    private boolean revoked = false;

    @Builder
    public RefreshToken(
            String token,
            String tokenId,
            Long userId,
            Instant expiryDate,
            String deviceId,
            String parentId,
            String ipAddress) {
        this.token = token;
        this.tokenId = tokenId;
        this.userId = userId;
        this.expiryDate = expiryDate;
        this.deviceId = deviceId == null ? "unknown" : deviceId;
        this.ipAddress = ipAddress == null ? "unknown" : deviceId;
        this.parentId = parentId;
        this.revoked = false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    public void update(String newToken, Instant newExpiry) {
        this.token = newToken;
        this.expiryDate = newExpiry;
    }
}
