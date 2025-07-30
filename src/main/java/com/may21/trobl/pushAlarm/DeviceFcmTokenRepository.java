package com.may21.trobl.pushAlarm;

import com.may21.trobl.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DeviceFcmTokenRepository extends JpaRepository<DeviceFcmToken, Long> {

    @Query("SELECT DISTINCT d.fcmToken FROM DeviceFcmToken d WHERE d.user.id IN :userIds")
    List<String> findFcmTokensByUserIds(List<Long> userIds);

    @Modifying
    @Transactional
    void deleteByFcmToken(String fcmToken);

    boolean existsByUserIdAndFcmToken(Long userId, String fcmToken);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
