package com.may21.trobl.recordLimit.service;

import com.may21.trobl.recordLimit.domain.RecordTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface RecordTrackRepository extends JpaRepository<RecordTrack, Long> {
    List<RecordTrack> findByUserIdAndAiGeneratedIsTrueAndCreatedAtAfter(Long userId,
            LocalDateTime thisMonth);

    int countByUserIdAndAiGeneratedIsTrueAndCreatedAtAfter(Long userId, LocalDateTime thisMonth);

    boolean existsByRecordId(String recordId);

    Optional<RecordTrack> findByRecordId(String recordId);
}
