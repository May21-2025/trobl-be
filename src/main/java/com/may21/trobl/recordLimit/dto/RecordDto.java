package com.may21.trobl.recordLimit.dto;

import lombok.Getter;

public class RecordDto {

    @Getter
    public static class Usage {
        private Long userId;
        private int limitCount;
        private int usedCount;

        public Usage(Long userId, int limitCount, int usedCount) {
            this.userId = userId;
            this.limitCount = limitCount;
            this.usedCount = usedCount;
        }
    }

    @Getter
    public static class TrackRecord {
        private String recordId;
    }
}
