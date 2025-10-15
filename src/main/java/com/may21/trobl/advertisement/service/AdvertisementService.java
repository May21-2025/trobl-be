package com.may21.trobl.advertisement.service;

import com.may21.trobl._global.enums.BannerType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.advertisement.domain.AdRecord;
import com.may21.trobl.advertisement.domain.Advertisement;
import com.may21.trobl.advertisement.domain.Brand;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import com.may21.trobl.advertisement.repository.AdRecordRepository;
import com.may21.trobl.advertisement.repository.AdvertisementRepository;
import com.may21.trobl.advertisement.repository.BrandRepository;
import com.may21.trobl.user.domain.User;
import lombok.RequiredArgsConstructor;
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

    private final AdvertisementRepository advertisementRepository;
    private final AdRecordRepository adRecordRepository;
    private final BrandRepository brandRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    @Transactional
    public AdvertisementDto.Response getRandomAdvertisement(BannerType bannerType, Long userId,
            Long bannerId) {

        Map<Long, Long> todayDistribution = getTodayDistribution(bannerType);

        List<Advertisement> activeAds = bannerId == null ?
                advertisementRepository.findActiveAdvertisementsByBannerType(bannerType) :
                advertisementRepository.findActiveAdvertisementsByBannerTypeExceptBannerId(bannerId,
                        bannerType);

        if (activeAds.isEmpty()) return new AdvertisementDto.Response();
        Map<Long, Double> targetRatio = calculateTargetRatio(activeAds);

        Advertisement selectedAd =
                selectByDistributionGap(activeAds, todayDistribution, targetRatio);
        Brand advertisement = selectedAd.getBrand();
        String brandName = advertisement.getBrandName();
        recordAdView(selectedAd, userId, brandName);

        return new AdvertisementDto.Response(advertisement, selectedAd);
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
        }
        else {
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
    public Map<Long, Long> getTodayDistribution(BannerType bannerType) {
        Map<Long, Long> distribution = new HashMap<>();

        // Redis에서 오늘의 노출 데이터 조회 (type 반영)
        String pattern = "ad_views:" + bannerType.name()
                .toLowerCase() + ":*:" + LocalDate.now();
        Set<String> keys = redisTemplate.keys(pattern);

        for (String key : keys) {
            try {
                // key 형식: "ad_views:{type}:{adId}:{date}"
                String[] parts = key.split(":");
                if (parts.length >= 4) {
                    Long adId = Long.parseLong(parts[2]);  // 0=ad_views, 1=type, 2=adId, 3=date
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

    private Advertisement selectByDistributionGap(List<Advertisement> ads,
            Map<Long, Long> todayDistribution, Map<Long, Double> targetRatio) {

        long totalViews = todayDistribution.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

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
    public void recordAdView(Advertisement ad, Long userId, String brandName) {

        CompletableFuture.runAsync(() -> {
            AdRecord adRecord = new AdRecord(ad, userId, brandName);
            adRecordRepository.save(adRecord);
        });

        // adType을 포함한 키 형식으로 수정
        String todayKey = "ad_views:" + ad.getBannerType()
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
    public AdvertisementDto.BrandInfo createBrand(AdvertisementDto.BrandRequest requestDto) {

        Brand advertisement = new Brand(requestDto);
        advertisement = brandRepository.save(advertisement);


        return new AdvertisementDto.BrandInfo(advertisement);
    }

    // 2단계: 이미지 URL 업데이트
    @Transactional
    public AdvertisementDto.BannerList updateBannerImages(Long brandId, List<String> imageUrls) {
        Brand advertisement = brandRepository.findById(brandId)
                .orElseThrow(
                        () -> new RuntimeException("Advertisement not found with id: " + brandId));

        List<Advertisement> advertisements = advertisementRepository.findByBrandId(brandId);

        // 이미지 URL 업데이트
        for (int i = 0; i < advertisements.size() && i < imageUrls.size(); i++) {
            Advertisement banner = advertisements.get(i);
            banner.setImageUrl(imageUrls.get(i));
        }
        advertisements = advertisementRepository.saveAll(advertisements);

        List<AdvertisementDto.AdvertisementInfo> advertisementInfos = new ArrayList<>();
        for (Advertisement banner : advertisements) {
            advertisementInfos.add(new AdvertisementDto.AdvertisementInfo(banner));
        }

        return new AdvertisementDto.BannerList(new AdvertisementDto.BrandInfo(advertisement),
                advertisementInfos);
    }


    @Transactional(readOnly = true)
    public Page<AdvertisementDto.BrandInfo> getBrands(int size, int page, String sortType,
            boolean asc) {
        Pageable pageable = asc ? PageRequest.of(page, size, Sort.by(sortType)
                .ascending()) : PageRequest.of(page, size, Sort.by(sortType)
                .descending());
        Page<Brand> advertisements = brandRepository.findAll(pageable);
        return advertisements.map(AdvertisementDto.BrandInfo::new);
    }

    @Transactional(readOnly = true)
    public List<AdvertisementDto.AdvertisementInfo> getAdvertisementBanners(Long brandId) {
        List<Advertisement> advertisements = advertisementRepository.findByBrandId(brandId);
        List<AdvertisementDto.AdvertisementInfo> advertisementInfos = new ArrayList<>();
        for (Advertisement advertisement : advertisements) {
            advertisementInfos.add(new AdvertisementDto.AdvertisementInfo(advertisement));
        }
        return advertisementInfos;
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
        Long activeAdvertisements = brandRepository.countByActiveTrue();

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
    public AdvertisementDto.AdStats getAdvertisementStats(Long brandId) {
        Brand advertisement = brandRepository.findById(brandId)
                .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));

        // 해당 광고의 모든 배너 조회
        List<Advertisement> advertisements = advertisementRepository.findByBrandId(brandId);

        // 총 노출 수
        Long totalViews = adRecordRepository.countByAdvertisementIn(advertisements);

        // 총 클릭 수
        Long totalClicks = adRecordRepository.countByAdvertisementInAndClickedTrue(advertisements);

        // 클릭률 계산
        Double clickThroughRate = totalViews > 0 ? (double) totalClicks / totalViews : 0.0;

        // 고유 사용자 수
        Long uniqueUsers = adRecordRepository.countDistinctUserIdByAdvertisementIn(advertisements);

        // 배너별 통계
        List<AdvertisementDto.BannerStats> bannerStats = getBannerStats(advertisements);

        // 일별 통계 (최근 30일)
        List<AdvertisementDto.DailyStats> dailyStats = getDailyStatsByBanners(advertisements, 30);

        return new AdvertisementDto.AdStats(brandId, advertisement.getBrandName(), totalViews,
                totalClicks, clickThroughRate, uniqueUsers, bannerStats, dailyStats,
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
    private List<AdvertisementDto.DailyStats> getDailyStatsByBanners(
            List<Advertisement> advertisements, int days) {
        List<AdvertisementDto.DailyStats> dailyStats = new ArrayList<>();
        LocalDate endDate = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            Long views = adRecordRepository.countByAdvertisementInAndShowedAtBetween(advertisements,
                    date.atStartOfDay(), date.plusDays(1)
                            .atStartOfDay());
            Long clicks = adRecordRepository.countByAdvertisementInAndShowedAtBetweenAndClickedTrue(
                    advertisements, date.atStartOfDay(), date.plusDays(1)
                            .atStartOfDay());
            Long uniqueUsers =
                    adRecordRepository.countByAdvertisementInAndShowedAtBetweenAndClickedTrue(
                            advertisements, date.atStartOfDay(), date.plusDays(1)
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

        for (BannerType bannerType : BannerType.values()) {
            List<Advertisement> advertisements =
                    advertisementRepository.findByBannerType(bannerType);
            if (!advertisements.isEmpty()) {
                Long views = adRecordRepository.countByAdvertisementIn(advertisements);
                Long clicks =
                        adRecordRepository.countByAdvertisementInAndClickedTrue(advertisements);
                Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;
                Long advertisementCount = (long) advertisements.size();

                adTypeStats.add(new AdvertisementDto.AdTypeStats(bannerType.name(), views, clicks,
                        clickThroughRate, advertisementCount));
            }
        }

        return adTypeStats;
    }

    // 특정 배너들의 광고 타입별 통계
    private List<AdvertisementDto.AdTypeStats> getAdTypeStatsByBanners(
            List<Advertisement> advertisements) {
        Map<BannerType, List<Advertisement>> bannersByType = advertisements.stream()
                .collect(Collectors.groupingBy(Advertisement::getBannerType));

        List<AdvertisementDto.AdTypeStats> adTypeStats = new ArrayList<>();

        for (Map.Entry<BannerType, List<Advertisement>> entry : bannersByType.entrySet()) {
            BannerType bannerType = entry.getKey();
            List<Advertisement> typeAdvertisements = entry.getValue();

            Long views = adRecordRepository.countByAdvertisementIn(typeAdvertisements);
            Long clicks =
                    adRecordRepository.countByAdvertisementInAndClickedTrue(typeAdvertisements);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;
            Long advertisementCount = (long) typeAdvertisements.size();

            adTypeStats.add(new AdvertisementDto.AdTypeStats(bannerType.name(), views, clicks,
                    clickThroughRate, advertisementCount));
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
            Long advertisementCount = (long) brandRepository.findByBrandName(brandName)
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
    private List<AdvertisementDto.BannerStats> getBannerStats(List<Advertisement> advertisements) {
        List<AdvertisementDto.BannerStats> bannerStats = new ArrayList<>();

        for (Advertisement advertisement : advertisements) {
            Long views = adRecordRepository.countByAdvertisement(advertisement);
            Long clicks = adRecordRepository.countByAdvertisementAndClickedTrue(advertisement);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;

            bannerStats.add(new AdvertisementDto.BannerStats(advertisement.getId(),
                    advertisement.getBannerType()
                            .name(), views, clicks, clickThroughRate, advertisement.getImageUrl()));
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

        for (BannerType bannerType : BannerType.values()) {
            // 브랜드별 특정 광고 타입의 통계 계산
            Long views = getBrandAdTypeViews(brandName, bannerType);
            Long clicks = getBrandAdTypeClicks(brandName, bannerType);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;
            Long advertisementCount = getBrandAdTypeCount(brandName, bannerType);

            if (views > 0) {
                adTypeStats.add(new AdvertisementDto.AdTypeStats(bannerType.name(), views, clicks,
                        clickThroughRate, advertisementCount));
            }
        }

        return adTypeStats;
    }

    // 브랜드별 특정 날짜의 광고 타입별 통계
    private List<AdvertisementDto.AdTypeStats> getBrandAdTypeStatsByDate(String brandName,
            LocalDate date) {
        List<AdvertisementDto.AdTypeStats> adTypeStats = new ArrayList<>();

        for (BannerType bannerType : BannerType.values()) {
            Long views = getBrandAdTypeViewsByDate(brandName, bannerType, date);
            Long clicks = getBrandAdTypeClicksByDate(brandName, bannerType, date);
            Double clickThroughRate = views > 0 ? (double) clicks / views : 0.0;

            if (views > 0) {
                adTypeStats.add(new AdvertisementDto.AdTypeStats(bannerType.name(), views, clicks,
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
        return (long) brandRepository.findByBrandName(brandName)
                .size();
    }

    // 브랜드별 광고 타입별 노출 수 조회 (헬퍼 메서드들)
    private Long getBrandAdTypeViews(String brandName, BannerType bannerType) {
        return adRecordRepository.countByBrandNameAndAdType(brandName, bannerType);
    }

    private Long getBrandAdTypeClicks(String brandName, BannerType bannerType) {
        return adRecordRepository.countByBrandNameAndAdTypeAndClickedTrue(brandName, bannerType);
    }

    private Long getBrandAdTypeCount(String brandName, BannerType bannerType) {
        return adRecordRepository.countByBrandNameAndAdType(brandName, bannerType);
    }

    private Long getBrandAdTypeViewsByDate(String brandName, BannerType bannerType,
            LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1)
                .atStartOfDay();
        return adRecordRepository.countByBrandNameAndAdTypeAndShowedAtBetween(brandName, bannerType,
                startOfDay, endOfDay);
    }

    private Long getBrandAdTypeClicksByDate(String brandName, BannerType bannerType,
            LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1)
                .atStartOfDay();
        return adRecordRepository.countByBrandNameAndAdTypeAndShowedAtBetweenAndClickedTrue(
                brandName, bannerType, startOfDay, endOfDay);
    }

    @Transactional
    public AdvertisementDto.AdvertisementInfo createAdvertisement(Long advertisementId,
            AdvertisementDto.BannerRequest request) {
        Brand advertisement = brandRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));

        Advertisement banner = new Advertisement(advertisement, request);
        banner = advertisementRepository.save(banner);

        return new AdvertisementDto.AdvertisementInfo(banner);
    }

    @Transactional
    public AdvertisementDto.AdvertisementInfo updateAdvertisement(Long advertisementId,
            Long bannerId, AdvertisementDto.BannerRequest request) {
        Brand advertisement = brandRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));

        Advertisement banner = advertisementRepository.findById(bannerId)
                .orElseThrow(() -> new RuntimeException("배너를 찾을 수 없습니다."));

        if (!banner.getBrand()
                .getId()
                .equals(advertisement.getId())) {
            throw new RuntimeException("배너가 해당 광고에 속하지 않습니다.");
        }

        banner.updateFromRequest(request);
        banner = advertisementRepository.save(banner);

        return new AdvertisementDto.AdvertisementInfo(banner);
    }

    public Brand getBrandById(Long advertisementId) {
        return brandRepository.findById(advertisementId)
                .orElseThrow(() -> new RuntimeException("광고를 찾을 수 없습니다."));
    }

    @Transactional
    public boolean deleteAdvertisement(Long advertisementId) {
        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.ADVERTISEMENT_NOT_FOUND));
        advertisementRepository.delete(advertisement);
        return true;
    }

    @Transactional
    public boolean clickAdvertisement(Long advertisementId, User user) {
        Advertisement advertisement = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.ADVERTISEMENT_NOT_FOUND));
        String userClickKey = null;
        if (user != null) {
            // 사용자가 이미 오늘 이 광고를 클릭했는지 확인
            userClickKey =
                    "user_ad_click:" + user.getId() + ":" + advertisementId + ":" + LocalDate.now();
            Boolean hasClicked = redisTemplate.hasKey(userClickKey);
            if (hasClicked) {return false;}
        }
        // 클릭 기록 저장
        CompletableFuture.runAsync(() -> {
            Optional<AdRecord> optionalAdRecord = user == null ? Optional.empty() :
                    adRecordRepository.findTopByAdvertisementAndUserIdOrderByShowedAtDesc(
                            advertisement, user.getId());
            if (optionalAdRecord.isPresent()) {
                AdRecord adRecord = optionalAdRecord.get();
                adRecord.setClicked(true);
                adRecordRepository.save(adRecord);
            }
        });
        if (user != null) {
            redisTemplate.opsForValue()
                    .set(userClickKey, "1", Duration.ofHours(3));

        }

        return true;
    }
}
