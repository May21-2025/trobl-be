package com.may21.trobl.advertisement.service;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl.advertisement.domain.AdRecord;
import com.may21.trobl.advertisement.domain.Advertisement;
import com.may21.trobl.advertisement.domain.Banner;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import com.may21.trobl.advertisement.repository.AdRecordRepository;
import com.may21.trobl.advertisement.repository.AdRepository;
import com.may21.trobl.advertisement.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final BannerRepository bannerRepository;
    private final AdRecordRepository adRecordRepository;
    private final AdRepository adRepository;

    private RedisTemplate<String, Object> redisTemplate;

    private final Random random = new Random();


    @Transactional
    public AdvertisementDto.Response getRandomAdvertisement(AdType adType, Long userId,
            Long bannerId) {

        Map<Long, Long> todayDistribution = getTodayDistribution(adType);

        List<Banner> activeAds = bannerId == null ? bannerRepository.findActiveAdvertisements() :
                bannerRepository.findActiveAdvertisementsExceptBannerId(bannerId);

        Map<Long, Double> targetRatio = calculateTargetRatio(activeAds);

        Banner selectedAd = selectByDistributionGap(activeAds, todayDistribution, targetRatio);
        ;
        Advertisement advertisement = selectedAd.getAdvertisement();
        recordAdView(selectedAd, userId);

        return new AdvertisementDto.Response(advertisement, selectedAd);
    }


    /**
     * 목표 분배율 계산
     */
    private Map<Long, Double> calculateTargetRatio(List<Banner> activeAds) {
        Map<Long, Double> targetRatio = new HashMap<>();

        // 총 가중치 계산
        int totalWeight = activeAds.stream()
                .mapToInt(Banner::getWeight)
                .sum();

        if (totalWeight == 0) {
            // 가중치가 모두 0이면 균등 분배
            double equalRatio = 1.0 / activeAds.size();
            for (Banner ad : activeAds) {
                targetRatio.put(ad.getId(), equalRatio);
            }
        }
        else {
            // 가중치 기반 목표 비율 계산
            for (Banner ad : activeAds) {
                double ratio = (double) ad.getWeight() / totalWeight;
                targetRatio.put(ad.getId(), ratio);
            }
        }

        return targetRatio;
    }

    /**
     * 오늘의 광고별 노출 분배 현황 조회
     */
    private Map<Long, Long> getTodayDistribution(AdType adType) {
        Map<Long, Long> distribution = new HashMap<>();

        // Redis에서 오늘의 노출 데이터 조회 (adType 반영)
        String pattern = "ad_views:" + adType.name()
                .toLowerCase() + ":*:" + LocalDate.now();
        Set<String> keys = redisTemplate.keys(pattern);

        for (String key : keys) {
            try {
                // key 형식: "ad_views:{adType}:{adId}:{date}"
                String[] parts = key.split(":");
                if (parts.length >= 4) {
                    Long adId = Long.parseLong(parts[2]);  // 0=ad_views, 1=adType, 2=adId, 3=date
                    Object viewsObj = redisTemplate.opsForValue()
                            .get(key);
                    Long views = viewsObj != null ? Long.parseLong(viewsObj.toString()) : 0L;
                    distribution.put(adId, views);
                }
            } catch (NumberFormatException e) {
                // 잘못된 형식의 키는 무시
                continue;
            }
        }

        return distribution;
    }

    private Banner selectByDistributionGap(List<Banner> ads, Map<Long, Long> todayDistribution,
            Map<Long, Double> targetRatio) {

        long totalViews = todayDistribution.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        Banner bestAd = null;
        double maxGap = -1;

        for (Banner ad : ads) {
            long currentViews = todayDistribution.getOrDefault(ad.getId(), 0L);
            double currentRatio = totalViews > 0 ? (double) currentViews / totalViews : 0;
            double targetRatio_val = targetRatio.getOrDefault(ad.getId(), 0.0);

            // 목표 비율과 현재 비율의 차이 (부족한 정도)
            double gap = targetRatio_val - currentRatio;

            if (gap > maxGap) {
                maxGap = gap;
                bestAd = ad;
            }
        }

        return bestAd != null ? bestAd : ads.get(0);
    }

    /**
     * 노출 기록
     */
    private void recordAdView(Banner ad, Long userId) {

        CompletableFuture.runAsync(() -> {
            AdRecord adRecord = new AdRecord(ad, userId);
            adRecordRepository.save(adRecord);
        });

        String todayKey = "ad_views:" + ad.getId() + ":" + LocalDate.now();
        redisTemplate.opsForValue()
                .increment(todayKey);
        redisTemplate.expire(todayKey, Duration.ofDays(1));

        String userViewKey = "user_ad_view:" + userId + ":" + ad.getId() + ":" + LocalDate.now();
        redisTemplate.opsForValue()
                .set(userViewKey, "1", Duration.ofDays(1));
    }

    public AdvertisementDto.BannerList createAdvertisementAndBanners(
            AdvertisementDto.CreateAdvertisement requestDto, List<String> imageUrls) {
        AdvertisementDto.AdvertisementRequest advertisementRequest =
                requestDto.getAdvertisementRequest();
        List<AdvertisementDto.BannerRequest> bannerRequests = requestDto.getBannerRequestList();

        Advertisement advertisement = new Advertisement(advertisementRequest);
        advertisement = adRepository.save(advertisement);

        List<Banner> banners = new ArrayList<>();
        for (AdvertisementDto.BannerRequest bannerRequest : bannerRequests) {
            Banner banner =
                    new Banner(advertisement, bannerRequest.getType(), bannerRequest.getWeight());
            banners.add(banner);
        }
        banners = bannerRepository.saveAll(banners);

        List<AdvertisementDto.BannerInfo> bannerInfos = new ArrayList<>();
        for (Banner banner : banners) {
            bannerInfos.add(new AdvertisementDto.BannerInfo(banner));
        }

        return new AdvertisementDto.BannerList(
                new AdvertisementDto.AdvertisementInfo(advertisement), bannerInfos);
    }
}
