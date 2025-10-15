package com.may21.trobl.advertisement.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.enums.BannerType;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.advertisement.domain.Brand;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import com.may21.trobl.advertisement.service.AdvertisementService;
import com.may21.trobl.storage.StorageService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdController {

    private static final Logger log = LoggerFactory.getLogger(AdController.class);

    private final AdvertisementService advertisementService;
    private final JwtTokenUtil jwtTokenUtil;
    private final StorageService storageService;
    private final UserService userService;


    @GetMapping("")
    public ResponseEntity<Message> getAdImages(@RequestParam String type,
            @RequestParam(required = false) Long advertisementId,
            @RequestHeader("Authorization") String token) {

        Long userId = token != null ? jwtTokenUtil.getUserIdFromToken(token) : null;
        BannerType bannerTypeEnum = BannerType.valueOf(type.toUpperCase());
        AdvertisementDto.Response response =
                advertisementService.getRandomAdvertisement(bannerTypeEnum, userId,
                        advertisementId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<Message> createBrands(@RequestBody AdvertisementDto.BrandRequest request,
            @RequestHeader("Authorization") String token) {
        try {

            jwtTokenUtil.getAdminUserByToken(token);
            AdvertisementDto.BrandInfo response = advertisementService.createBrand(request);
            return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error in createAdvertisement: " + e.getMessage());
            e.printStackTrace();
            log.info("Error in createAdvertisement: " + e.getMessage());
            return new ResponseEntity<>(Message.success(e.getMessage()), HttpStatus.OK);
        }
    }

    @GetMapping("/brands")
    public ResponseEntity<Message> getBrand(@RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "createdAt") String sortType,
            @RequestParam(defaultValue = "true") boolean asc,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        Page<AdvertisementDto.BrandInfo> response =
                advertisementService.getBrands(size, page, sortType, asc);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/brands/{brandId}/advertisements")
    public ResponseEntity<Message> getAdvertisements(@PathVariable Long brandId,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<AdvertisementDto.AdvertisementInfo> response =
                advertisementService.getAdvertisementBanners(brandId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/brands/{brandId}/advertisements")
    public ResponseEntity<Message> createAdvertisement(@PathVariable Long brandId,
            @RequestPart(value = "file") MultipartFile file,
            @RequestPart(value = "request") AdvertisementDto.BannerRequest request,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdvertisementDto.AdvertisementInfo response =
                advertisementService.createAdvertisement(brandId, request);
        Brand brand = advertisementService.getBrandById(brandId);
        String image = storageService.uploadBannerImage(brand, response, file);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/brands/{brandId}/advertisements/{advertisementId}")
    public ResponseEntity<Message> updateAdvertisement(@PathVariable Long brandId,
            @PathVariable Long advertisementId,
            @RequestPart(value = "request") AdvertisementDto.BannerRequest request,
            @RequestPart(value = "file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        Brand ad = advertisementService.getBrandById(brandId);
        AdvertisementDto.AdvertisementInfo response =
                advertisementService.updateAdvertisement(brandId, advertisementId, request);
        String image = storageService.uploadBannerImage(ad, response, file);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/brands/{brandId}/advertisements/{advertisementId}")
    public ResponseEntity<Message> deleteAdvertisement(@PathVariable Long advertisementId,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = advertisementService.deleteAdvertisement(advertisementId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/advertisements/{advertisementId}/click")
    public ResponseEntity<Message> clickAdvertisement(@PathVariable Long advertisementId,
            @RequestHeader("Authorization") String token) {
        Long userId = token != null ? jwtTokenUtil.getUserIdFromToken(token) : null;
        User user = userId != null ? userService.getUser(userId) : null;
        boolean response = advertisementService.clickAdvertisement(advertisementId, user);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/stats")
    public ResponseEntity<Message> getAdStats(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdvertisementDto.AllStats response = advertisementService.getAdvertisementAllStats();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/stats/{brandId}")
    public ResponseEntity<Message> getBrandStatsById(@PathVariable Long brandId,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdvertisementDto.AdStats response = advertisementService.getAdvertisementStats(brandId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/stats/brands/{brandName}")
    public ResponseEntity<Message> getBrandStatsByName(@PathVariable String brandName,
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

}
