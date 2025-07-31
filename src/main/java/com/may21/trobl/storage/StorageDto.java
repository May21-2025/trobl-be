package com.may21.trobl.storage;

import lombok.Getter;

import java.time.LocalDateTime;

public class StorageDto {

    @Getter
    public static class ProfileImage{
        private String imageKey;
        private LocalDateTime profileImageUpdated;

    }
}
