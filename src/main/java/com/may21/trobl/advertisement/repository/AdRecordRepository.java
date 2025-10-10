package com.may21.trobl.advertisement.repository;

import com.may21.trobl._global.enums.BannerType;
import com.may21.trobl.advertisement.domain.AdRecord;
import com.may21.trobl.advertisement.domain.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdRecordRepository extends JpaRepository<AdRecord, Long> {

    // 클릭된 레코드 수 조회
    long countByClickedTrue();

    // 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar")
    long countDistinctUserId();

    // 특정 배너들의 레코드 수 조회
    long countByAdvertisementIn(List<Advertisement> advertisements);

    // 특정 배너들 중 클릭된 레코드 수 조회
    long countByAdvertisementInAndClickedTrue(List<Advertisement> advertisements);

    // 특정 배너들의 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar WHERE ar.advertisement IN :banners")
    long countDistinctUserIdByAdvertisementIn(@Param("banners") List<Advertisement> advertisements);

    // 특정 배너의 레코드 수 조회
    long countByAdvertisement(Advertisement advertisement);

    // 특정 배너의 클릭된 레코드 수 조회
    long countByAdvertisementAndClickedTrue(Advertisement advertisement);

    // 특정 기간의 레코드 수 조회
    long countByShowedAtBetween(LocalDateTime start, LocalDateTime end);

    // 특정 기간의 클릭된 레코드 수 조회
    long countByShowedAtBetweenAndClickedTrue(LocalDateTime start, LocalDateTime end);

    // 특정 기간의 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar WHERE ar.showedAt BETWEEN :start AND :end")
    long countDistinctUserIdByShowedAtBetween(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // 특정 배너들의 특정 기간 레코드 수 조회
    long countByAdvertisementInAndShowedAtBetween(List<Advertisement> advertisements, LocalDateTime start,
            LocalDateTime end);

    // 특정 배너들의 특정 기간 클릭된 레코드 수 조회
    long countByAdvertisementInAndShowedAtBetweenAndClickedTrue(List<Advertisement> advertisements, LocalDateTime start,
            LocalDateTime end);

    // 특정 배너들의 특정 기간 고유 사용자 수 조회
    @Query("SELECT COUNT(DISTINCT ar.userId) FROM AdRecord ar WHERE ar.advertisement IN :banners AND ar.showedAt BETWEEN :start AND :end")
    long countDistinctUserIdByAdvertisementInAndShowedAtBetween(@Param("banners") List<Advertisement> advertisements,
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

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.advertisement.brand a WHERE a.brandName = " +
            ":brandName")
    Long countByBrandName(String brandName);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar  WHERE ar.brandName = :brandName AND ar.clicked = true")
    Long countByBrandNameAndClickedTrue(String brandName);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar  WHERE ar.brandName = :brandName AND ar.showedAt BETWEEN :start AND :end")
    Long countByBrandNameAndShowedAtBetween(String brandName, LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT COUNT(ar) FROM AdRecord ar  WHERE ar.brandName = :brandName AND ar.showedAt BETWEEN :start AND :end AND ar.clicked = true")
    Long countByBrandNameAndShowedAtBetweenAndClickedTrue(String brandName, LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.advertisement b WHERE ar.brandName = :brandName AND b.bannerType = :bannerType")
    Long countByBrandNameAndAdType(String brandName, BannerType bannerType);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.advertisement b WHERE ar.brandName = :brandName AND b.bannerType = :bannerType AND ar.clicked = true")
    Long countByBrandNameAndAdTypeAndClickedTrue(String brandName, BannerType bannerType);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.advertisement b WHERE ar.brandName = :brandName " +
            "AND b.bannerType = :bannerType AND ar.showedAt BETWEEN :startOfDay AND :endOfDay")
    Long countByBrandNameAndAdTypeAndShowedAtBetween(String brandName, BannerType bannerType,
            LocalDateTime startOfDay, LocalDateTime endOfDay);

    @Query("SELECT COUNT(ar)  FROM AdRecord ar JOIN ar.advertisement b WHERE ar.brandName = :brandName " +
            "AND b.bannerType = :bannerType AND ar.showedAt BETWEEN :startOfDay AND :endOfDay AND ar.clicked" +
            " = true")
    Long countByBrandNameAndAdTypeAndShowedAtBetweenAndClickedTrue(String brandName, BannerType bannerType,
            LocalDateTime startOfDay, LocalDateTime endOfDay);

    Optional<AdRecord> findTopByAdvertisementAndUserIdOrderByShowedAtDesc(Advertisement advertisement, Long id);
}
