package com.may21.trobl.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadUserProfileImage(Long userId, MultipartFile file);

}
