package com.may21.trobl.storage;

import com.may21.trobl.advertisement.dto.AdvertisementDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {
    String uploadUserProfileImage(Long userId, MultipartFile file);

    List<String> uploadBannerImages(List<MultipartFile> adImages,
            AdvertisementDto.CreateAdvertisement request);
}
