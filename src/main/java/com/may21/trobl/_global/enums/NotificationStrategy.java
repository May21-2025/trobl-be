package com.may21.trobl._global.enums;

// 알림 전송 전략 enum
public enum NotificationStrategy {
    IMMEDIATE,    // 즉시 전송
    BATCHED,     // 10분마다 일괄 전송
    SCHEDULED    // 예약 전송
}