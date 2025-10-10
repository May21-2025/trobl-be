package com.may21.trobl.advertisement.repository;

import com.may21.trobl._global.enums.BannerType;
import com.may21.trobl.advertisement.domain.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {


    List<Advertisement> findByBrandId(Long brand);

    // 광고 타입으로 배너 조회
    List<Advertisement> findByBannerType(BannerType bannerType);

    @Query("SELECT b FROM Advertisement b JOIN b.brand a WHERE a.active = true And b.bannerType = :bannerType")
    List<Advertisement> findActiveAdvertisementsByBannerType(BannerType bannerType);

    @Query("SELECT b FROM Advertisement b JOIN b.brand a WHERE a.active = true And b.id <> " +
            ":bannerId And b.bannerType = :bannerType")
    List<Advertisement> findActiveAdvertisementsByBannerTypeExceptBannerId(Long bannerId, BannerType bannerType);
}
