package com.may21.trobl.user.domain;

import com.may21.trobl._global.enums.OAuthProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);


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

    @Query("SELECT EXISTS (SELECT 1 FROM User u WHERE u.nickname = :nickname)")
    boolean existsByNickname(String nickname);

    Optional<User> findPartnerById(Long id);

    List<User> findPartnerAndUserById(Long userId);

    @Query("SELECT u FROM User u WHERE u.provider IS NOT NULL")
    List<User> findAllOAuth();


    @Query("SELECT u.provider FROM User u WHERE u.username = :username")
    OAuthProvider getOAuthByUsername(String username);

    int countByUnregistered(boolean b);

    long countBySignUpDateAfter(LocalDate weekAgo);

    long countByLastLoginDateAfter(LocalDate thirtyDaysAgo);

    Page<User> findByTestUserIsTrue(Pageable pageable);

    Page<User> findByTestUserIsFalseAndUnregisteredIsFalse(Pageable pageable);

    @Query("SELECT u.id FROM User u WHERE u.testUser = true")
    List<Long> findUserIdsByTestUserIsTrue();

    @Query("SELECT COUNT(u) > 0  FROM User u WHERE u.id = :userId AND u.testUser = true")
    boolean isVirtualUser(Long userId);

    Optional<User> findByIdAndTestUserIsTrue(Long userId);

    List<User> findUserByTestUserIsTrue();

    @Query("SELECT u FROM User u  WHERE u.id IN (" +
            "SELECT MIN(u2.id) FROM User u2 WHERE u.testUser = true AND LOWER(u2.nickname) LIKE " +
            "LOWER(CONCAT('%', " + ":keyword, '%')) GROUP BY u2.nickname" + ")")
    List<User> searchUsersByKeywordTestUserIsTrue(String keyword);
}