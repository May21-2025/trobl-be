package com.may21.trobl.advertisement.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.enums.AdType;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import com.may21.trobl.advertisement.service.AdvertisementService;
import com.may21.trobl.storage.StorageService;
import com.may21.trobl.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdvertisementService advertisementService;
    private final JwtTokenUtil jwtTokenUtil;
    private final StorageService storageService;

    @GetMapping("")
    public ResponseEntity<Message> getAdImages(@RequestParam String adType,
            @RequestParam(required = false) Long bannerId, @AuthenticationPrincipal User user) {
        AdType adTypeEnum = AdType.valueOf(adType.toUpperCase());
        Long userId = (user != null) ? user.getId() : null;
        AdvertisementDto.Response response =
                advertisementService.getRandomAdvertisement(adTypeEnum, userId, bannerId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<Message> createAdvertisementAndBanners(
            @RequestBody AdvertisementDto.CreateAdvertisement requestDto,
            @RequestPart List<MultipartFile> adImages,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<String> images = storageService.uploadBannerImages(adImages, requestDto);
        AdvertisementDto.BannerList response =
                advertisementService.createAdvertisementAndBanners(requestDto, images);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
