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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final BannerRepository bannerRepository;
    private final AdRecordRepository adRecordRepository;
    private final AdRepository adRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    @Transactional
    public AdvertisementDto.Response getRandomAdvertisement(AdType adType, Long userId,
            Long bannerId, String brandName) {

        Map<Long, Long> todayDistribution = getTodayDistribution(adType);

        List<Banner> activeAds = bannerId == null ? bannerRepository.findActiveAdvertisements() :
                bannerRepository.findActiveAdvertisementsExceptBannerId(bannerId);

        Map<Long, Double> targetRatio = calculateTargetRatio(activeAds);

        Banner selectedAd = selectByDistributionGap(activeAds, todayDistribution, targetRatio);
        Advertisement advertisement = selectedAd.getAdvertisement();
        recordAdView(selectedAd, userId, brandName);

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
    public Map<Long, Long> getTodayDistribution(AdType adType) {
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
    public void recordAdView(Banner ad, Long userId, String brandName) {

        CompletableFuture.runAsync(() -> {
            AdRecord adRecord = new AdRecord(ad, userId, brandName);
            adRecordRepository.save(adRecord);
        });

        // adType을 포함한 키 형식으로 수정
        String todayKey = "ad_views:" + ad.getAdType()
                .name()
                .toLowerCase() + ":" + ad.getId() + ":" + LocalDate.now();
        redisTemplate.opsForValue()
                .increment(todayKey);
        redisTemplate.expire(todayKey, Duration.ofDays(1));

        String userViewKey = "user_ad_view:" + userId + ":" + ad.getId() + ":" + LocalDate.now();
        redisTemplate.opsForValue()
                .set(userViewKey, "1", Duration.ofDays(1));
    }

    // 1단계: JSON 광고 정보만 저장 (이미지 없이)
    @Transactional
    public AdvertisementDto.BannerList createAdvertisement(AdvertisementDto.CreateAdvertisement requestDto) {
        AdvertisementDto.AdvertisementRequest advertisementRequest = requestDto.getAdvertisementRequest();
        List<AdvertisementDto.BannerRequest> bannerRequests = requestDto.getBannerRequestList();

        Advertisement advertisement = new Advertisement(advertisementRequest);
        advertisement = adRepository.save(advertisement);

        List<Banner> banners = new ArrayList<>();
        if (bannerRequests != null && !bannerRequests.isEmpty()) {
            for (AdvertisementDto.BannerRequest bannerRequest : bannerRequests) {
                Banner banner = new Banner(advertisement, bannerRequest);
                banners.add(banner);
            }
            banners = bannerRepository.saveAll(banners);
        }

        List<AdvertisementDto.BannerInfo> bannerInfos = new ArrayList<>();
        for (Banner banner : banners) {
            bannerInfos.add(new AdvertisementDto.BannerInfo(banner));
        }

        return new AdvertisementDto.BannerList(
                new AdvertisementDto.AdvertisementInfo(advertisement), bannerInfos);
    }

    // 2단계: 이미지 URL 업데이트
    @Transactional
    public AdvertisementDto.BannerList updateBannerImages(Long advertisementId, List<String> imageUrls) {
        Advertisement advertisement = adRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("Advertisement not found with id: " + advertisementId));

        List<Banner> banners = bannerRepository.findByAdvertisementId(advertisementId);
        
        // 이미지 URL 업데이트
        for (int i = 0; i < banners.size() && i < imageUrls.size(); i++) {
            Banner banner = banners.get(i);
            banner.setImageUrl(imageUrls.get(i));
        }
        banners = bannerRepository.saveAll(banners);

        List<AdvertisementDto.BannerInfo> bannerInfos = new ArrayList<>();
        for (Banner banner : banners) {
            bannerInfos.add(new AdvertisementDto.BannerInfo(banner));
        }

        return new AdvertisementDto.BannerList(
                new AdvertisementDto.AdvertisementInfo(advertisement), bannerInfos);
    }

    @Transactional
    public AdvertisementDto.BannerList createAdvertisementAndBanners(
            AdvertisementDto.CreateAdvertisement requestDto, List<String> imageUrls) {
        // DTO에서 데이터 추출 (getter 메서드가 없으므로 리플렉션 사용하거나 DTO 수정 필요)
        AdvertisementDto.AdvertisementRequest advertisementRequest =
                requestDto.getAdvertisementRequest();
        List<AdvertisementDto.BannerRequest> bannerRequests = requestDto.getBannerRequestList();

        Advertisement advertisement = new Advertisement(advertisementRequest);
        advertisement = adRepository.save(advertisement);

        List<Banner> banners = new ArrayList<>();
        for (AdvertisementDto.BannerRequest bannerRequest : bannerRequests) {
            Banner banner = new Banner(advertisement, bannerRequest);
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

    @Transactional(readOnly = true)
    public Page<AdvertisementDto.AdvertisementInfo> getAdvertisements(int size, int page,
            String sortType, boolean asc) {
        Pageable pageable = asc ? PageRequest.of(page, size, Sort.by(sortType)
                .ascending()) : PageRequest.of(page, size, Sort.by(sortType)
                .descending());
        Page<Advertisement> advertisements = adRepository.findAll(pageable);
        return advertisements.map(AdvertisementDto.AdvertisementInfo::new);
    }

    @Transactional(readOnly = true)
    public List<AdvertisementDto.BannerInfo> getAdvertisementBanners(Long advertisementId) {
        List<Banner> banners = bannerRepository.findByAdvertisementId(advertisementId);
        List<AdvertisementDto.BannerInfo> bannerInfos = new ArrayList<>();
        for (Banner banner : banners) {
            bannerInfos.add(new AdvertisementDto.BannerInfo(banner));
        }
        return bannerInfos;
    }

    // 전체 광고 통계 조회
    @Transactional(readOnly = true)
    public AdvertisementDto.AllStats getAdvertisementAllStats() {
        // 전체 노출 수
        Long totalViews = adRecordRepository.count();

        // 전체 클릭 수
        Long totalClicks = adRecordRepository.countByClickedTrue();

        // 클릭률 계산
        Double clickThroughRate = totalViews > 0 ? (double) totalClicks / totalViews : 0.0;

        // 노출된 총 사용자 수
        Long totalUsers = adRecordRepository.countDistinctUserId();

        // 활성 광고 수
        Long activeAdvertisements = adRepository.countByActiveTrue();

        // 일별 통계 (최근 7일)
        List<AdvertisementDto.DailyStats> dailyStats = getDailyStats(7);

        // 광고 타입별 통계
        List<AdvertisementDto.AdTypeStats> adTypeStats = getAdTypeStats();

        // 상위 브랜드 통계 (상위 10개)
        List<AdvertisementDto.BrandStats> topBrands = getTopBrands(10);

        return new AdvertisementDto.AllStats(totalViews, totalClicks, clickThroughRate, totalUsers,
                activeAdvertisements, dailyStats, adTypeStats, topBrands);
    }

    // 개별 광고 통계 조회
    @Transactional(readOnly = true)
    public AdvertisementDto.AdStats getAdvertisementStats(Long advertisementId) {
        Advertisement advertisement = adRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));

        // 해당 광고의 모든 배너 조회
        List<Banner> banners = bannerRepository.findByAdvertisementId(advertisementId);

        // 총 노출 수
        Long totalViews = adRecordRepository.countByBannerIn(banners);

        // 총 클릭 수
        Long totalClicks = adRecordRepository.countByBannerInAndClickedTrue(banners);

        // 클릭률 계산
        Double clickThroughRate = totalViews > 0 ? (double) totalClicks / totalViews : 0.0;

        // 고유 사용자 수
        Long uniqueUsers = adRecordRepository.countDistinctUserIdByBannerIn(banners);

        // 배너별 통계
        List<AdvertisementDto.BannerStats> bannerStats = getBannerStats(banners);

        // 일별 통계 (최근 30일)
        List<AdvertisementDto.DailyStats> dailyStats = getDailyStatsByBanners(banners, 30);

        return new AdvertisementDto.AdStats(advertisementId, advertisement.getBrandName(),
                totalViews, totalClicks, clickThroughRate, uniqueUsers, bannerStats, dailyStats,
                advertisement.getStartDate(), advertisement.getEndDate(),
                advertisement.getActive());
    }

    // 일별 통계 조회 (최근 N일)
    private List<AdvertisementDto.DailyStats> getDailyStats(int days) {
        List<AdvertisementDto.DailyStats> dailyStats = new ArrayList<>();
        LocalDate endDate = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            Long views = adRecordRepository.countByShowedAtBetween(date.atStartOfDay(),
                    date.plusDays(1)
                            .atStartOfDay());
            Long clicks =
                    adRecordRepository.countByShowedAtBetweenAndClickedTrue(date.atStartOfDay(),
                            date.plusDays(1)
                                    .atStartOfDay());
            Long uniqueUsers =
                    adRecordRepository.countDistinctUserIdByShowedAtBetween(date.atStartOfDay(),
                            date.plusDays(1)
                                    .atStartOfDay());
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;

            dailyStats.add(new AdvertisementDto.DailyStats(date, views, clicks, clickThroughRate,
                    uniqueUsers));
        }

        return dailyStats;
    }

    // 특정 배너들의 일별 통계
    private List<AdvertisementDto.DailyStats> getDailyStatsByBanners(List<Banner> banners,
            int days) {
        List<AdvertisementDto.DailyStats> dailyStats = new ArrayList<>();
        LocalDate endDate = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            Long views = adRecordRepository.countByBannerInAndShowedAtBetween(banners,
                    date.atStartOfDay(), date.plusDays(1)
                            .atStartOfDay());
            Long clicks =
                    adRecordRepository.countByBannerInAndShowedAtBetweenAndClickedTrue(banners,
                            date.atStartOfDay(), date.plusDays(1)
                                    .atStartOfDay());
            Long uniqueUsers =
                    adRecordRepository.countDistinctUserIdByBannerInAndShowedAtBetween(banners,
                            date.atStartOfDay(), date.plusDays(1)
                                    .atStartOfDay());
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;

            dailyStats.add(new AdvertisementDto.DailyStats(date, views, clicks, clickThroughRate,
                    uniqueUsers));
        }

        return dailyStats;
    }

    // 광고 타입별 통계
    private List<AdvertisementDto.AdTypeStats> getAdTypeStats() {
        List<AdvertisementDto.AdTypeStats> adTypeStats = new ArrayList<>();

        for (AdType adType : AdType.values()) {
            List<Banner> banners = bannerRepository.findByAdType(adType);
            if (!banners.isEmpty()) {
                Long views = adRecordRepository.countByBannerIn(banners);
                Long clicks = adRecordRepository.countByBannerInAndClickedTrue(banners);
                Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;
                Long advertisementCount = (long) banners.size();

                adTypeStats.add(new AdvertisementDto.AdTypeStats(adType.name(), views, clicks,
                        clickThroughRate, advertisementCount));
            }
        }

        return adTypeStats;
    }

    // 특정 배너들의 광고 타입별 통계
    private List<AdvertisementDto.AdTypeStats> getAdTypeStatsByBanners(List<Banner> banners) {
        Map<AdType, List<Banner>> bannersByType = banners.stream()
                .collect(Collectors.groupingBy(Banner::getAdType));

        List<AdvertisementDto.AdTypeStats> adTypeStats = new ArrayList<>();

        for (Map.Entry<AdType, List<Banner>> entry : bannersByType.entrySet()) {
            AdType adType = entry.getKey();
            List<Banner> typeBanners = entry.getValue();

            Long views = adRecordRepository.countByBannerIn(typeBanners);
            Long clicks = adRecordRepository.countByBannerInAndClickedTrue(typeBanners);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;
            Long advertisementCount = (long) typeBanners.size();

            adTypeStats.add(
                    new AdvertisementDto.AdTypeStats(adType.name(), views, clicks, clickThroughRate,
                            advertisementCount));
        }

        return adTypeStats;
    }

    // 상위 브랜드 통계
    private List<AdvertisementDto.BrandStats> getTopBrands(int limit) {
        // 브랜드별 통계를 계산하고 상위 N개 반환
        List<String> brandNames = adRecordRepository.findDistinctBrandNames();
        List<AdvertisementDto.BrandStats> brandStats = new ArrayList<>();

        for (String brandName : brandNames) {
            Long views = adRecordRepository.countByBrandName(brandName);
            Long clicks = adRecordRepository.countByBrandNameAndClickedTrue(brandName);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;
            Long advertisementCount = (long) adRepository.findByBrandName(brandName)
                    .size();

            // 간단한 AdTypeStats 리스트 생성 (실제로는 더 복잡한 로직 필요)
            List<AdvertisementDto.AdTypeStats> adTypeBreakdown = new ArrayList<>();
            List<AdvertisementDto.BrandDailyStats> dailyStats = new ArrayList<>();
            List<AdvertisementDto.BrandHourlyStats> hourlyStats = new ArrayList<>();
            List<AdvertisementDto.BrandStats> topBrands = new ArrayList<>();

            brandStats.add(
                    new AdvertisementDto.BrandStats(brandName, views, clicks, clickThroughRate, 0L,
                            advertisementCount, dailyStats, hourlyStats, adTypeBreakdown, topBrands,
                            null, null));
        }

        // 노출 수 기준으로 정렬하고 상위 N개 반환
        return getBrandStats(limit, brandStats);
    }

    // 배너별 통계
    private List<AdvertisementDto.BannerStats> getBannerStats(List<Banner> banners) {
        List<AdvertisementDto.BannerStats> bannerStats = new ArrayList<>();

        for (Banner banner : banners) {
            Long views = adRecordRepository.countByBanner(banner);
            Long clicks = adRecordRepository.countByBannerAndClickedTrue(banner);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;

            bannerStats.add(new AdvertisementDto.BannerStats(banner.getId(), banner.getAdType()
                    .name(), views, clicks, clickThroughRate, banner.getImageUrl()));
        }

        return bannerStats;
    }

    // 브랜드별 전체 통계 조회
    @Transactional(readOnly = true)
    public AdvertisementDto.BrandStats getBrandStats(String brandName) {
        // 브랜드별 총 노출 수
        Long totalViews = adRecordRepository.countByBrandName(brandName);

        // 브랜드별 총 클릭 수
        Long totalClicks = adRecordRepository.countByBrandNameAndClickedTrue(brandName);

        // 클릭률 계산
        Double clickThroughRate = totalViews > 0 ? (double) totalClicks / totalViews : 0.0;

        // 브랜드별 고유 사용자 수
        Long totalUsers = adRecordRepository.countDistinctUserIdByBrandName(brandName);

        // 브랜드별 활성 광고 수 (해당 브랜드의 모든 광고)
        Long activeAdvertisements = getActiveAdvertisementsByBrand(brandName);

        // 브랜드별 일별 통계 (최근 7일)
        List<AdvertisementDto.BrandDailyStats> dailyStats = getBrandDailyStats(brandName, 7);

        // 브랜드별 시간별 통계 (오늘)
        List<AdvertisementDto.BrandHourlyStats> hourlyStats =
                getBrandHourlyStats(brandName, LocalDate.now());

        // 브랜드별 광고 타입별 통계
        List<AdvertisementDto.AdTypeStats> adTypeStats = getBrandAdTypeStats(brandName);

        // 브랜드별 상위 브랜드 통계
        List<AdvertisementDto.BrandStats> topBrands = getProjectTopBrands(brandName, 10);

        // 브랜드별 첫/마지막 활동 시간
        LocalDateTime firstViewDate = adRecordRepository.findFirstActivityByBrandName(brandName);
        LocalDateTime lastViewDate = adRecordRepository.findLastActivityByBrandName(brandName);

        return new AdvertisementDto.BrandStats(brandName, totalViews, totalClicks, clickThroughRate,
                totalUsers, activeAdvertisements, dailyStats, hourlyStats, adTypeStats, topBrands,
                firstViewDate, lastViewDate);
    }

    // 브랜드 목록 통계 조회
    @Transactional(readOnly = true)
    public AdvertisementDto.BrandListStats getBrandListStats() {
        List<String> brandNames = adRecordRepository.findDistinctBrandNames();
        List<AdvertisementDto.BrandSummary> brandSummaries = new ArrayList<>();

        Long totalViews = 0L;
        Long totalClicks = 0L;

        for (String brandName : brandNames) {
            Long views = adRecordRepository.countByBrandName(brandName);
            Long clicks = adRecordRepository.countByBrandNameAndClickedTrue(brandName);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;
            Long activeAds = getActiveAdvertisementsByBrand(brandName);
            LocalDateTime lastActivity = adRecordRepository.findLastActivityByBrandName(brandName);

            brandSummaries.add(
                    new AdvertisementDto.BrandSummary(brandName, views, clicks, clickThroughRate,
                            activeAds, lastActivity));

            totalViews += views;
            totalClicks += clicks;
        }

        // 노출 수 기준으로 정렬
        brandSummaries.sort((a, b) -> {
            b.getTotalViews()
                    .compareTo(a.getTotalViews());
            return b.getTotalViews()
                    .compareTo(a.getTotalViews());
        });

        Double overallClickThroughRate = totalViews > 0 ? (double) totalClicks / totalViews : 0.0;

        return new AdvertisementDto.BrandListStats(brandSummaries, (long) brandNames.size(),
                totalViews, totalClicks, overallClickThroughRate);
    }

    // 브랜드별 일별 통계 조회
    @Transactional(readOnly = true)
    public List<AdvertisementDto.BrandDailyStats> getBrandDailyStats(String brandName, int days) {
        List<AdvertisementDto.BrandDailyStats> dailyStats = new ArrayList<>();
        LocalDate endDate = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            Long views = adRecordRepository.countByBrandNameAndShowedAtBetween(brandName,
                    date.atStartOfDay(), date.plusDays(1)
                            .atStartOfDay());
            Long clicks =
                    adRecordRepository.countByBrandNameAndShowedAtBetweenAndClickedTrue(brandName,
                            date.atStartOfDay(), date.plusDays(1)
                                    .atStartOfDay());
            Long uniqueUsers =
                    adRecordRepository.countDistinctUserIdByBrandNameAndShowedAtBetween(brandName,
                            date.atStartOfDay(), date.plusDays(1)
                                    .atStartOfDay());
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;

            // 해당 날짜의 광고 타입별 분석
            List<AdvertisementDto.AdTypeStats> adTypeBreakdown =
                    getBrandAdTypeStatsByDate(brandName, date);

            dailyStats.add(new AdvertisementDto.BrandDailyStats(brandName, date, views, clicks,
                    clickThroughRate, uniqueUsers, adTypeBreakdown));
        }

        return dailyStats;
    }

    // 브랜드별 시간별 통계 조회
    @Transactional(readOnly = true)
    public List<AdvertisementDto.BrandHourlyStats> getBrandHourlyStats(String brandName,
            LocalDate date) {
        List<AdvertisementDto.BrandHourlyStats> hourlyStats = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            Long views = adRecordRepository.countByBrandNameAndHourAndDate(brandName, hour, date);
            Long clicks =
                    adRecordRepository.countByBrandNameAndHourAndDateAndClickedTrue(brandName, hour,
                            date);
            Long uniqueUsers =
                    adRecordRepository.countDistinctUserIdByBrandNameAndHourAndDate(brandName, hour,
                            date);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;

            hourlyStats.add(
                    new AdvertisementDto.BrandHourlyStats(brandName, date, hour, views, clicks,
                            clickThroughRate, uniqueUsers));
        }

        return hourlyStats;
    }

    // 브랜드별 광고 타입별 통계
    private List<AdvertisementDto.AdTypeStats> getBrandAdTypeStats(String brandName) {
        // 브랜드별 모든 AdRecord 조회 후 광고 타입별로 그룹화
        // 이 부분은 실제 구현에서 AdRecord와 Banner를 조인해서 처리해야 함
        List<AdvertisementDto.AdTypeStats> adTypeStats = new ArrayList<>();

        for (AdType adType : AdType.values()) {
            // 브랜드별 특정 광고 타입의 통계 계산
            Long views = getBrandAdTypeViews(brandName, adType);
            Long clicks = getBrandAdTypeClicks(brandName, adType);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;
            Long advertisementCount = getBrandAdTypeCount(brandName, adType);

            if (views > 0) {
                adTypeStats.add(new AdvertisementDto.AdTypeStats(adType.name(), views, clicks,
                        clickThroughRate, advertisementCount));
            }
        }

        return adTypeStats;
    }

    // 브랜드별 특정 날짜의 광고 타입별 통계
    private List<AdvertisementDto.AdTypeStats> getBrandAdTypeStatsByDate(String brandName,
            LocalDate date) {
        List<AdvertisementDto.AdTypeStats> adTypeStats = new ArrayList<>();

        for (AdType adType : AdType.values()) {
            Long views = getBrandAdTypeViewsByDate(brandName, adType, date);
            Long clicks = getBrandAdTypeClicksByDate(brandName, adType, date);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;

            if (views > 0) {
                adTypeStats.add(new AdvertisementDto.AdTypeStats(adType.name(), views, clicks,
                        clickThroughRate, 0L));
            }
        }

        return adTypeStats;
    }

    // 브랜드별 상위 브랜드 통계
    private List<AdvertisementDto.BrandStats> getProjectTopBrands(String brandName, int limit) {
        // 브랜드별 브랜드 통계 계산
        List<AdvertisementDto.BrandStats> brandStats = new ArrayList<>();

        // 실제 구현에서는 브랜드별 AdRecord와 Banner, Advertisement를 조인해서 처리
        // 여기서는 간단한 예시만 제공

        return getBrandStats(limit, brandStats);
    }

    private List<AdvertisementDto.BrandStats> getBrandStats(int limit,
            List<AdvertisementDto.BrandStats> brandStats) {
        return brandStats.stream()
                .sorted((a, b) -> {
                    try {
                        java.lang.reflect.Field aField = a.getClass()
                                .getDeclaredField("totalViews");
                        aField.setAccessible(true);
                        Long aViews = (Long) aField.get(a);

                        java.lang.reflect.Field bField = b.getClass()
                                .getDeclaredField("totalViews");
                        bField.setAccessible(true);
                        Long bViews = (Long) bField.get(b);

                        return Long.compare(bViews, aViews);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    // 브랜드별 활성 광고 수 조회
    private Long getActiveAdvertisementsByBrand(String brandName) {
        return (long) adRepository.findByBrandName(brandName)
                .size();
    }

    // 브랜드별 광고 타입별 노출 수 조회 (헬퍼 메서드들)
    private Long getBrandAdTypeViews(String brandName, AdType adType) {
        return adRecordRepository.countByBrandNameAndAdType(brandName, adType);
    }

    private Long getBrandAdTypeClicks(String brandName, AdType adType) {
        return adRecordRepository.countByBrandNameAndAdTypeAndClickedTrue(brandName, adType);
    }

    private Long getBrandAdTypeCount(String brandName, AdType adType) {
        return adRecordRepository.countByBrandNameAndAdType(brandName, adType);
    }

    private Long getBrandAdTypeViewsByDate(String brandName, AdType adType, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1)
                .atStartOfDay();
        return adRecordRepository.countByBrandNameAndAdTypeAndShowedAtBetween(brandName, adType,
                startOfDay, endOfDay);
    }

    private Long getBrandAdTypeClicksByDate(String brandName, AdType adType, LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1)
                .atStartOfDay();
        return adRecordRepository.countByBrandNameAndAdTypeAndShowedAtBetweenAndClickedTrue(
                brandName, adType, startOfDay, endOfDay);
    }

    @Transactional
    public AdvertisementDto.BannerInfo createBanner(Long advertisementId,
            AdvertisementDto.BannerRequest request) {
        Advertisement advertisement = adRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));

        Banner banner = new Banner(advertisement, request);
        banner = bannerRepository.save(banner);

        return new AdvertisementDto.BannerInfo(banner);
    }

    public AdvertisementDto.BannerInfo updateBanner(Long advertisementId, Long bannerId,
            AdvertisementDto.BannerRequest request) {
        Advertisement advertisement = adRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new RuntimeException("배너를 찾을 수 없습니다."));

        if (!banner.getAdvertisement()
                .getId()
                .equals(advertisement.getId())) {
            throw new RuntimeException("배너가 해당 광고에 속하지 않습니다.");
        }

        banner.updateFromRequest(request);
        banner = bannerRepository.save(banner);

        return new AdvertisementDto.BannerInfo(banner);
    }

    public Advertisement getAdvertisementById(Long advertisementId) {
        return adRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));
    }
}
