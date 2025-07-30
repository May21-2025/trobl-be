package com.may21.trobl.pushAlarm;

import com.google.firebase.messaging.*;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final UserRepository userRepository;
    private final FirebaseMessaging firebaseMessaging;
    private final DeviceFcmTokenRepository deviceFcmTokenRepository;

    /**
     * FCM 알림 전송 - 앱 상태에 따라 자동으로 처리됨
     * - 앱 실행 중: data-only 메시지로 인앱 알림
     * - 백그라운드: 자동으로 푸시 알림으로 표시
     */
    @Async
    public void sendNotificationTo(List<String> fcmTokenList, NotificationDto.SendRequest request) {
        for (String fcmToken : fcmTokenList) {
            // 유효한 FCM 토큰 필터링
            if (fcmToken == null || fcmToken.trim()
                    .isEmpty()) {
                continue; // 유효하지 않은 토큰은 건너뜀
            }

            try {
                Map<String, String> data = createNotificationData(request);

                Message message = Message.builder()
                        .setToken(fcmToken)
                        .putAllData(data)
                        //  앱 상태에 따라 자동 처리
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setTitle(request.getTitle())
                                        .setBody(request.getBody())
                                        .setSound("default")
                                        .setChannelId("trobl_notifications")
                                        .build())
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder()
                                        .setAlert(ApsAlert.builder()
                                                .setTitle(request.getTitle())
                                                .setBody(request.getBody())
                                                .build())
                                        .setSound("default")
                                        .build())
                                .build())
                        .build();

                String response = firebaseMessaging.send(message);
                log.error("FCM notification sent successfully to user {}: {}", request.getUserId(),
                        response);
            } catch (FirebaseMessagingException e) {
                String errorCode = e.getMessagingErrorCode()
                        .name();
                log.error("""
                        =======================================
                        Failed to send FCM notification to token: {} - errorCode: {}
                        Detail : {}=======================================
                        """, fcmToken, errorCode, e.toString());

                if (isInvalidTokenError(errorCode)) {
                    deleteFcmTokenFromDatabase(fcmToken);
                    log.debug("Deleted invalid FCM token: {}", fcmToken);
                }

                handleFCMError(e, request.getUserId());
            } catch (Exception e) {
                log.error("Unexpected error while sending FCM notification to user {}",
                        request.getUserId(), e);
            }

        }

    }

    private void deleteFcmTokenFromDatabase(String fcmToken) {
        deviceFcmTokenRepository.deleteByFcmToken(fcmToken);
    }

    private boolean isInvalidTokenError(String errorCode) {
        return errorCode != null &&
                (errorCode.equals("registration-token-not-registered") || // 토큰 만료, 삭제
                        errorCode.equals("invalid-argument") ||                  // 유효하지 않은 형식
                        errorCode.equals("unregistered")                         // iOS 등에서 발생 가능
                );
    }


    private Map<String, String> createNotificationData(NotificationDto.SendRequest request) {
        Map<String, String> data = new HashMap<>();

        data.put("type", request.getNotificationType()
                .name()
                .toLowerCase());
        data.put("userId", request.getUserId()
                .toString());
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
        data.put("itemId", request.getItemId() != null ? request.getItemId()
                .toString() : "");
        data.put("itemType", request.getItemType() != null ? request.getItemType()
                .toString()
                .toLowerCase() : "");

        return data;
    }

    private void handleFCMError(FirebaseMessagingException e, Long userId) {
        String errorCode = e.getMessagingErrorCode()
                .name();

        switch (errorCode) {
            case "UNREGISTERED":
            case "INVALID_REGISTRATION_TOKEN":
                log.warn("Invalid FCM token for user {}, removing from database", userId);
                break;

            case "MESSAGE_RATE_EXCEEDED":
                log.warn("Message rate exceeded for user {}", userId);
                // 재시도 로직 또는 지연 처리
                break;

            case "DEVICE_MESSAGE_RATE_EXCEEDED":
                log.warn("Device message rate exceeded for user {}", userId);
                break;

            default:
                log.error("FCM error for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateFcmToken(String fcmToken) {
        try {
            // 테스트 메시지로 토큰 유효성 검증
            Message testMessage = Message.builder()
                    .setToken(fcmToken)
                    .putData("type", "token_validation")
                    .build();

            firebaseMessaging.send(testMessage, true); // dry run
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("Invalid FCM token: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    public boolean registerToken(NotificationDto.TokenRegistrationRequest request, Long userId) {
        try {
            if (deviceFcmTokenRepository.existsByUserIdAndFcmToken(userId, request.getFcmToken())) {
                return true; // 이미 등록된 토큰은 무시
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
            DeviceFcmToken deviceFcmToken = new DeviceFcmToken(user, request.getFcmToken());
            deviceFcmTokenRepository.save(deviceFcmToken);
            return true;
        } catch (Exception e) {
            log.error("Failed to register FCM token for user {}", userId, e);
            return false;
        }
    }
}