package com.may21.trobl.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);

    @Modifying
    @Transactional
    @Query(
            "UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.id != :currentTokenId")
    void revokeAllUserTokensExceptCurrent(Long userId, Long currentTokenId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId")
    void revokeAllUserTokens(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.token = :token")
    void revokeToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    void deleteAllExpiredTokens(Instant now);

    @Query("SELECT COUNT(r) > 0 FROM RefreshToken r WHERE r.token = :token AND r.revoked = true")
    boolean isTokenRevoked(String token);

    Optional<RefreshToken> findByUserIdAndDeviceId(Long userId, String deviceId);

    @Query("SELECT r FROM RefreshToken r WHERE r.userId = :userId AND r.deviceId = :deviceId AND r.revoked = false")
    List<RefreshToken> findValidTokenByUserIdAndDeviceId(Long userId, String deviceId);
}
