package com.may21.trobl.recordLimit.repository;

import com.may21.trobl.recordLimit.domain.RecordTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RecordTrackRepository extends JpaRepository<RecordTrack, Long> {

    int countByUserIdAndAiGeneratedIsTrueAndCreatedAtAfter(Long userId, LocalDateTime thisMonth);

    boolean existsByRecordId(String recordId);

    Optional<RecordTrack> findByRecordId(String recordId);

    // 일별 통계를 위한 배치 쿼리
    @Query("SELECT r FROM RecordTrack r " +
            "WHERE r.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY r.createdAt")
    List<RecordTrack> findAllBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

}