package com.may21.trobl.advertisement.repository;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl.advertisement.domain.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("SELECT b FROM Banner b JOIN b.advertisement a WHERE a.active = true")
    List<Banner> findActiveAdvertisements();

    @Query("SELECT b FROM Banner b JOIN b.advertisement a WHERE a.active = true And b.id <> :bannerId")
    List<Banner> findActiveAdvertisementsExceptBannerId(Long bannerId);

    List<Banner> findByAdvertisementId(Long advertisementId);
    
    // 광고 타입으로 배너 조회
    List<Banner> findByAdType(AdType adType);
}
