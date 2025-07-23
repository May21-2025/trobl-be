package com.may21.trobl.user.domain;

import com.may21.trobl._global.enums.OAuthProvider;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username")
    void incrementFailedLoginAttempts(String username);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.failedLoginAttempts = 0 WHERE u.username = :username")
    void resetFailedLoginAttempts(String username);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountNonLocked = :locked WHERE u.username = :username")
    void updateAccountLockStatus(String username, boolean locked);

    List<User> findByIdIn(List<Long> userIds);

    boolean existsByNickname(String nickname);

    Optional<User> findPartnerById(Long id);

    List<User> findPartnerAndUserById(Long userId);

    @Query("SELECT u FROM User u WHERE u.provider IS NOT NULL")
    List<User> findAllOAuth();

    @Query("SELECT u.fcmToken FROM User u WHERE u.id IN :userIds AND u.fcmToken IS NOT NULL")
    List<String> findFcmTokensByIds(List<Long> userIds);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.fcmToken = null WHERE u.id = :userId")
    void clearFcmToken(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.fcmToken = null WHERE u.fcmToken = :token")
    void clearFcmTokenByToken(String token);

    OAuthProvider getOAuthByUsername(String username, Sort sort);

    OAuthProvider getOAuthByUsername(String username);
}