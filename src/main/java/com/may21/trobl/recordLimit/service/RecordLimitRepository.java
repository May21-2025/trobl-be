package com.may21.trobl.recordLimit.service;

import com.may21.trobl.recordLimit.domain.RecordLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

import java.time.LocalDateTime;
import java.util.List;

interface RecordLimitRepository extends JpaRepository<RecordLimit, Long> {
    boolean existsByUserIdAndExpiryDateBefore(Long userId,LocalDateTime now);

    List<RecordLimit> findByUserId(Long userId);
}
