package com.may21.trobl.advertisement.service;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl.advertisement.domain.AdRecord;
import com.may21.trobl.advertisement.domain.Advertisement;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import com.may21.trobl.advertisement.repository.AdRecordRepository;
import com.may21.trobl.advertisement.repository.AdRepository;
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

    private final AdRepository advertisementRepository;
    private final AdRecordRepository adRecordRepository;

    private RedisTemplate<String, Object> redisTemplate;

    private final Random random = new Random();


    @Transactional
    public AdvertisementDto.Response getRandomAdvertisement(AdType adType, Long userId) {
        List<Advertisement> activeAds = advertisementRepository
                .findActiveAdvertisements();

        Advertisement selectedAd = selectByWeight(activeAds);

        recordAdView(selectedAd, adType, userId);

        return convertToDto(selectedAd, adType);
    }

    private static AdvertisementDto.Response convertToDto(Advertisement selectedAd, AdType adType) {
        return new AdvertisementDto.Response(selectedAd, adType);
    }

    /**
     * 가중치 기반 선택 알고리즘
     */
    private Advertisement selectByWeight(List<Advertisement> ads) {
        // 총 가중치 계산
        int totalWeight = ads.stream()
                .mapToInt(Advertisement::getWeight)
                .sum();

        // 랜덤 값 생성
        int randomValue = random.nextInt(totalWeight);
        int currentWeight = 0;

        // 가중치 기반 선택
        for (Advertisement ad : ads) {
            currentWeight += ad.getWeight();
            if (randomValue < currentWeight) {
                return ad;
            }
        }

        return ads.get(0); // fallback
    }


    /**
     * 라운드 로빈 방식 (더 공평한 분배)
     */
    public AdvertisementDto.Response getRoundRobinAdvertisement(AdType adType, Long userId) {
        String redisKey = "ad_round_robin:" + adType.name();

        Integer currentIndex = (Integer) redisTemplate.opsForValue().get(redisKey);
        if (currentIndex == null) {
            currentIndex = 0;
        }

        List<Advertisement> activeAds = advertisementRepository
                .findActiveAdvertisements();

        Advertisement selectedAd = activeAds.get(currentIndex % activeAds.size());

        redisTemplate.opsForValue().set(redisKey, (currentIndex + 1) % activeAds.size());

        recordAdView(selectedAd, adType, userId);

        return convertToDto(selectedAd, adType);
    }

    /**
     * 스마트 분배 (가중치 + 라운드로빈 혼합)
     */
    public AdvertisementDto.Response getSmartAdvertisement(AdType adType, Long userId) {
        String distributionKey = "ad_distribution:" + adType.name();

        Map<Long, Long> todayDistribution = getTodayDistribution(adType);

        List<Advertisement> activeAds = advertisementRepository
                .findActiveAdvertisements();

        Map<Long, Double> targetRatio = calculateTargetRatio(activeAds);

        // 현재 분배율과 목표 분배율 비교하여 선택
        Advertisement selectedAd = selectByDistributionGap(activeAds, todayDistribution, targetRatio);

        recordAdView(selectedAd, adType, userId);

        return convertToDto(selectedAd, adType);
    }

    /**
     * 목표 분배율 계산
     */
    private Map<Long, Double> calculateTargetRatio(List<Advertisement> activeAds) {
        Map<Long, Double> targetRatio = new HashMap<>();

        // 총 가중치 계산
        int totalWeight = activeAds.stream()
                .mapToInt(Advertisement::getWeight)
                .sum();

        if (totalWeight == 0) {
            // 가중치가 모두 0이면 균등 분배
            double equalRatio = 1.0 / activeAds.size();
            for (Advertisement ad : activeAds) {
                targetRatio.put(ad.getId(), equalRatio);
            }
        } else {
            // 가중치 기반 목표 비율 계산
            for (Advertisement ad : activeAds) {
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

        // Redis에서 오늘의 노출 데이터 조회
        String pattern = "ad_views:*:" + LocalDate.now();
        Set<String> keys = redisTemplate.keys(pattern);

        for (String key : keys) {
            try {
                // key 형식: "ad_views:광고ID:날짜"
                String[] parts = key.split(":");
                if (parts.length >= 2) {
                    Long adId = Long.parseLong(parts[1]);
                    Object viewsObj = redisTemplate.opsForValue().get(key);
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

    private Advertisement selectByDistributionGap(
            List<Advertisement> ads,
            Map<Long, Long> todayDistribution,
            Map<Long, Double> targetRatio) {

        long totalViews = todayDistribution.values().stream().mapToLong(Long::longValue).sum();

        Advertisement bestAd = null;
        double maxGap = -1;

        for (Advertisement ad : ads) {
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
    private void recordAdView(Advertisement ad, AdType adType, Long userId) {

        CompletableFuture.runAsync(() -> {
            AdRecord adRecord = new AdRecord(ad, adType, userId);
            adRecordRepository.save(adRecord);
        });

        String todayKey = "ad_views:" + ad.getId() + ":" + LocalDate.now();
        redisTemplate.opsForValue().increment(todayKey);
        redisTemplate.expire(todayKey, Duration.ofDays(1));

        String userViewKey = "user_ad_view:" + userId + ":" + ad.getId() + ":" + LocalDate.now();
        redisTemplate.opsForValue().set(userViewKey, "1", Duration.ofDays(1));
    }

}
