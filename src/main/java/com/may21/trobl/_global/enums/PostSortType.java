package com.may21.trobl._global.enums;

public enum PostSortType {
    TOTAL_ENGAGEMENT,     // 공감 + 댓글 + 뷰 종합
    REGION_ENGAGEMENT,    // 지역 공감 + 댓글 + 뷰 종합
    VIEW_COUNT,           // 조회수
    LIKE_COUNT,           // 공감 수
    PARTICIPANT_COUNT,    // 참여 인원
    LIKE_COMMENT_COUNT;   // 공감 + 댓글

    public static PostSortType fromString(String value) {
        return switch (value.toUpperCase()) {
            case "TOTAL_ENGAGEMENT" -> TOTAL_ENGAGEMENT;
            case "REGION_ENGAGEMENT" -> REGION_ENGAGEMENT;
            case "VIEW_COUNT" -> VIEW_COUNT;
            case "LIKE_COUNT" -> LIKE_COUNT;
            case "PARTICIPANT_COUNT" -> PARTICIPANT_COUNT;
            case "LIKE_COMMENT_COUNT" -> LIKE_COMMENT_COUNT;
            default -> throw new IllegalArgumentException("Unknown sort type: " + value);
        };
    }
}
