package com.may21.trobl.advertisement.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.enums.AdType;
import com.may21.trobl.advertisement.dto.AdvertisementDto;
import com.may21.trobl.advertisement.service.AdvertisementService;
import com.may21.trobl.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ads")
public class AdController {

    private final AdvertisementService advertisementService;

    public AdController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @GetMapping("")
    public ResponseEntity<Message> getAdImages(@RequestParam String adType, @AuthenticationPrincipal User user) {
        AdType adTypeEnum = AdType.valueOf(adType.toUpperCase());
        Long userId = (user != null) ? user.getId() : null;
        AdvertisementDto.Response response = advertisementService.getRandomAdvertisement(adTypeEnum, userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
