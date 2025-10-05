package com.may21.trobl.advertisement.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.enums.AdType;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.advertisement.domain.Advertisement;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import com.may21.trobl.advertisement.service.AdvertisementService;
import com.may21.trobl.storage.StorageService;
import com.may21.trobl.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;


@Slf4j

@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdvertisementService advertisementService;
    private final JwtTokenUtil jwtTokenUtil;
    private final StorageService storageService;

    @GetMapping("")
    public ResponseEntity<Message> getAdImages(@RequestParam String adType,
            @RequestParam(required = false) Long bannerId,
            @RequestParam(required = false) String brandName, @AuthenticationPrincipal User user) {
        AdType adTypeEnum = AdType.valueOf(adType.toUpperCase());
        Long userId = (user != null) ? user.getId() : null;
        AdvertisementDto.Response response =
                advertisementService.getRandomAdvertisement(adTypeEnum, userId, bannerId,
                        brandName);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/brands")
    public ResponseEntity<Message> getBrands(@RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "createdAt") String sortType,
            @RequestParam(defaultValue = "true") boolean asc,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        Page<AdvertisementDto.AdvertisementInfo> response =
                advertisementService.getAdvertisements(size, page, sortType, asc);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/brands/{advertisementId}")
    public ResponseEntity<Message> getBrands(@PathVariable Long advertisementId,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<AdvertisementDto.BannerInfo> response =
                advertisementService.getAdvertisementBanners(advertisementId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/brands/{advertisementId}/banners")
    public ResponseEntity<Message> createBanner(@PathVariable Long advertisementId,
            @RequestBody AdvertisementDto.BannerRequest request,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdvertisementDto.BannerInfo response =
                advertisementService.createBanner(advertisementId, request);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/brands/{advertisementId}/banners/{bannerId}")
    public ResponseEntity<Message> updateBanner(@PathVariable Long advertisementId,
            @PathVariable Long bannerId, @RequestPart AdvertisementDto.BannerRequest request,
            @RequestPart MultipartFile file, @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        Advertisement ad = advertisementService.getAdvertisementById(advertisementId);
        String image = storageService.uploadBannerImage(ad, request, file);
        AdvertisementDto.BannerInfo response =
                advertisementService.updateBanner(advertisementId, bannerId, request);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @GetMapping("/stats")
    public ResponseEntity<Message> getAdStats(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdvertisementDto.AllStats response = advertisementService.getAdvertisementAllStats();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/stats/{advertisementId}")
    public ResponseEntity<Message> getAdvertisementStats(@PathVariable Long advertisementId,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdvertisementDto.AdStats response =
                advertisementService.getAdvertisementStats(advertisementId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/stats/brands/{brandName}")
    public ResponseEntity<Message> getBrandStats(@PathVariable String brandName,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdvertisementDto.BrandStats response = advertisementService.getBrandStats(brandName);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    // 프로젝트별 통계 API들
    @GetMapping("/stats/brands")
    public ResponseEntity<Message> getBrandListStats(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdvertisementDto.BrandListStats response = advertisementService.getBrandListStats();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @GetMapping("/stats/brands/{brandName}/daily")
    public ResponseEntity<Message> getBrandDailyStats(@PathVariable String brandName,
            @RequestParam(defaultValue = "7") int days,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<AdvertisementDto.BrandDailyStats> response =
                advertisementService.getBrandDailyStats(brandName, days);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/stats/brands/{brandName}/hourly")
    public ResponseEntity<Message> getBrandHourlyStats(@PathVariable String brandName,
            @RequestParam(required = false) String date,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<AdvertisementDto.BrandHourlyStats> response =
                advertisementService.getBrandHourlyStats(brandName, targetDate);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    // 1단계: JSON 광고 정보 저장 API
    @PostMapping("")
    public ResponseEntity<Message> createAdvertisement(
            @RequestBody AdvertisementDto.CreateAdvertisement request,
            @RequestHeader("Authorization") String token) {
        try {
            System.out.println("=== 광고 생성 API 요청 디버깅 ===");
            System.out.println("request: " + request);
            System.out.println("=========================");

            jwtTokenUtil.getAdminUserByToken(token);
            AdvertisementDto.BannerList response =
                    advertisementService.createAdvertisement(request);
            return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error in createAdvertisement: " + e.getMessage());
            e.printStackTrace();
            log.info("Error in createAdvertisement: " + e.getMessage());
            return new ResponseEntity<>(Message.success(e.getMessage()), HttpStatus.OK);
        }
    }

    // 2단계: 파일 업로드 API
    @PostMapping("/{advertisementId}/images")
    public ResponseEntity<Message> uploadAdImages(@PathVariable Long advertisementId,
            @RequestPart List<MultipartFile> adImages,
            @RequestHeader("Authorization") String token) {
        try {
            System.out.println("=== 파일 업로드 API 요청 디버깅 ===");
            System.out.println("advertisementId: " + advertisementId);
            System.out.println("adImages count: " + (adImages != null ? adImages.size() : 0));
            if (adImages != null) {
                for (int i = 0; i < adImages.size(); i++) {
                    MultipartFile file = adImages.get(i);
                    System.out.println(
                            "  adImages[" + i + "]: " + file.getOriginalFilename() + " (" +
                                    file.getSize() + " bytes)");
                }
            }
            System.out.println("=========================");

            jwtTokenUtil.getAdminUserByToken(token);
            List<String> imageUrls = storageService.uploadBannerImages(advertisementId, adImages);
            AdvertisementDto.BannerList response =
                    advertisementService.updateBannerImages(advertisementId, imageUrls);
            return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error in uploadAdImages: " + e.getMessage());
            e.printStackTrace();
            log.info("Error in uploadAdImages: " + e.getMessage());
            return new ResponseEntity<>(Message.success(e.getMessage()), HttpStatus.OK);
        }
    }
}
