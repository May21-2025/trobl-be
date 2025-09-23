package com.may21.trobl.recordLimit.repository;

import com.may21.trobl.recordLimit.domain.RecordLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordLimitRepository extends JpaRepository<RecordLimit, Long> {
    boolean existsByUserIdAndExpiryDateBefore(Long userId, LocalDateTime now);

    List<RecordLimit> findByUserId(Long userId);
}
