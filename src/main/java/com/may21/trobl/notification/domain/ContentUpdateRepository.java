package com.may21.trobl.notification.domain;

import com.may21.trobl._global.enums.ItemType;
import jdk.jfr.Registered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Registered
public interface ContentUpdateRepository extends JpaRepository<ContentUpdate, Long> {
    @Modifying
    @Transactional
    void deleteAllByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, ItemType itemType);

    @Query("SELECT COUNT(c) > 0 FROM ContentUpdate c WHERE c.userId = :commentUserId AND c.targetId = :targetId AND c.targetType = :itemType")
    boolean existByUserIdAndTargetIdAndTargetType(Long commentUserId, Long targetId, ItemType itemType);

    @Query("SELECT c FROM ContentUpdate c WHERE c.userId = :userId AND c.targetType = :itemType AND c.targetId IN :targetIds")
    List<ContentUpdate> findByUserIdInTargetIdsAndTargetType(Long userId, List<Long> targetIds, ItemType itemType);

    boolean existsByUserId(Long userId);

    List<ContentUpdate> findByUserId(Long userId);
}
