package com.may21.trobl.advertisement.repository;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl.advertisement.domain.AdRecord;
import com.may21.trobl.advertisement.domain.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdRecordRepository extends JpaRepository<AdRecord, Long> {

    // 클릭된 레코드 수 조회
    long countByClickedTrue();

    // 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar")
    long countDistinctUserId();

    // 특정 배너들의 레코드 수 조회
    long countByBannerIn(List<Banner> banners);

    // 특정 배너들 중 클릭된 레코드 수 조회
    long countByBannerInAndClickedTrue(List<Banner> banners);

    // 특정 배너들의 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar WHERE ar.banner IN :banners")
    long countDistinctUserIdByBannerIn(@Param("banners") List<Banner> banners);

    // 특정 배너의 레코드 수 조회
    long countByBanner(Banner banner);

    // 특정 배너의 클릭된 레코드 수 조회
    long countByBannerAndClickedTrue(Banner banner);

    // 특정 기간의 레코드 수 조회
    long countByShowedAtBetween(LocalDateTime start, LocalDateTime end);

    // 특정 기간의 클릭된 레코드 수 조회
    long countByShowedAtBetweenAndClickedTrue(LocalDateTime start, LocalDateTime end);

    // 특정 기간의 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar WHERE ar.showedAt BETWEEN :start AND :end")
    long countDistinctUserIdByShowedAtBetween(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 특정 배너들의 특정 기간 레코드 수 조회
    long countByBannerInAndShowedAtBetween(List<Banner> banners, LocalDateTime start,
            LocalDateTime end);

    // 특정 배너들의 특정 기간 클릭된 레코드 수 조회
    long countByBannerInAndShowedAtBetweenAndClickedTrue(List<Banner> banners, LocalDateTime start,
            LocalDateTime end);

    // 특정 배너들의 특정 기간 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar WHERE ar.banner IN :banners AND ar.showedAt BETWEEN :start AND :end")
    long countDistinctUserIdByBannerInAndShowedAtBetween(@Param("banners") List<Banner> banners,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 프로젝트별 통계 메서드들

    // 프로젝트별 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar  WHERE ar.brandName = :brandName")
    long countDistinctUserIdByBrandName(@Param("brandName") String brandName);

    // 프로젝트별 특정 기간 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar  WHERE ar.brandName = :brandName AND ar.showedAt BETWEEN :start AND :end")
    long countDistinctUserIdByBrandNameAndShowedAtBetween(@Param("brandName") String brandName,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 프로젝트별 특정 시간대 레코드 수 조회
    @Query("SELECT COUNT(ar) FROM AdRecord ar  WHERE ar.brandName = :brandName AND HOUR(ar.showedAt) = :hour AND DATE(ar.showedAt) = :date")
    long countByBrandNameAndHourAndDate(@Param("brandName") String brandName,
            @Param("hour") int hour, @Param("date") LocalDate date);

    // 프로젝트별 특정 시간대 클릭된 레코드 수 조회
    @Query("SELECT COUNT(ar) FROM AdRecord ar  WHERE ar.brandName = :brandName AND HOUR(ar.showedAt) = :hour AND DATE(ar.showedAt) = :date AND ar.clicked = true")
    long countByBrandNameAndHourAndDateAndClickedTrue(@Param("brandName") String brandName,
            @Param("hour") int hour, @Param("date") LocalDate date);

    // 프로젝트별 특정 시간대 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar  WHERE ar.brandName = :brandName  AND HOUR(ar.showedAt) = :hour AND DATE(ar.showedAt) = :date")
    long countDistinctUserIdByBrandNameAndHourAndDate(@Param("brandName") String brandName,
            @Param("hour") int hour, @Param("date") LocalDate date);

    // 프로젝트 목록 조회
    @Query("SELECT DISTINCT ar.brandName FROM AdRecord ar")
    List<String> findDistinctBrandNames();

    // 프로젝트별 최근 활동 시간 조회
    @Query("SELECT MAX(ar.showedAt) FROM AdRecord ar  WHERE ar.brandName = :brandName ")
    LocalDateTime findLastActivityByBrandName(@Param("brandName") String brandName);

    // 프로젝트별 첫 활동 시간 조회
    @Query("SELECT MIN(ar.showedAt) FROM AdRecord ar WHERE " + "ar.brandName = :brandName")
    LocalDateTime findFirstActivityByBrandName(@Param("brandName") String brandName);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.banner.advertisement a WHERE a.brandName = :brandName")
    Long countByBrandName(String brandName);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar  WHERE ar.brandName = :brandName AND ar.clicked = true")
    Long countByBrandNameAndClickedTrue(String brandName);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar  WHERE ar.brandName = :brandName AND ar.showedAt BETWEEN :start AND :end")
    Long countByBrandNameAndShowedAtBetween(String brandName, LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT COUNT(ar) FROM AdRecord ar  WHERE ar.brandName = :brandName AND ar.showedAt BETWEEN :start AND :end AND ar.clicked = true")
    Long countByBrandNameAndShowedAtBetweenAndClickedTrue(String brandName, LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.banner b WHERE ar.brandName = :brandName AND b.adType = :adType")
    Long countByBrandNameAndAdType(String brandName, AdType adType);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.banner b WHERE ar.brandName = :brandName AND b.adType = :adType AND ar.clicked = true")
    Long countByBrandNameAndAdTypeAndClickedTrue(String brandName, AdType adType);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.banner b WHERE ar.brandName = :brandName " +
            "AND b.adType = :adType AND ar.showedAt BETWEEN :startOfDay AND :endOfDay")
    Long countByBrandNameAndAdTypeAndShowedAtBetween(String brandName, AdType adType,
            LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.banner b WHERE ar.brandName = :brandName " +
            "AND b.adType = :adType AND ar.showedAt BETWEEN :startOfDay AND :endOfDay AND ar.clicked = true")
    Long countByBrandNameAndAdTypeAndShowedAtBetweenAndClickedTrue(String brandName, AdType adType,
            LocalDateTime startOfDay, LocalDateTime endOfDay);
}
