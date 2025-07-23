package com.may21.trobl.pushAlarm;

import com.google.firebase.messaging.*;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final UserRepository userRepository;
    private final FirebaseMessaging firebaseMessaging;

    /**
     * FCM 알림 전송 - 앱 상태에 따라 자동으로 처리됨
     * - 앱 실행 중: data-only 메시지로 인앱 알림
     * - 백그라운드: 자동으로 푸시 알림으로 표시
     */
    public void sendNotificationTo(String fcmToken, NotificationDto.SendRequest request) {
        try {
            if (fcmToken == null || fcmToken.trim().isEmpty()) {
                return;
            }

            // data-only 메시지 생성
            Map<String, String> data = createNotificationData(request);

            Message message = Message.builder()
                    .setToken(fcmToken)
                    .putAllData(data)
                    // notification 필드를 포함하지 않음 - 이렇게 하면 앱 상태에 따라 자동 처리
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
            log.info("FCM notification sent successfully to user {}: {}", request.getUserId(), response);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to user {}: {}", request.getUserId(), e.getMessage());
            handleFCMError(e, request.getUserId());
        } catch (Exception e) {
            log.error("Unexpected error while sending FCM notification to user {}", request.getUserId(), e);
        }
    }

    /**
     * 배치 알림 전송 - 여러 사용자에게 동일한 메시지
     */
    public void sendBatchNotification(NotificationDto.BatchSendRequest request) {
        try {
            // FCM Token들 수집
            List<String> tokens = userRepository.findFcmTokensByIds(request.getUserIds())
                    .stream()
                    .filter(token -> token != null && !token.trim().isEmpty())
                    .toList();

            if (tokens.isEmpty()) {
                log.warn("No valid FCM tokens found for batch notification");
                return;
            }

            Map<String, String> data = createBatchNotificationData(request);

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .putAllData(data)
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

            BatchResponse response = firebaseMessaging.sendMulticast(message);
            log.info("Batch FCM notification sent: success={}, failure={}",
                    response.getSuccessCount(), response.getFailureCount());

            // 실패한 토큰들 처리
            handleBatchErrors(response, tokens);

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send batch FCM notification: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending batch FCM notification", e);
        }
    }

    private Map<String, String> createNotificationData(NotificationDto.SendRequest request) {
        Map<String, String> data = new HashMap<>();

        // 필수 데이터
        data.put("type", "notification");
        data.put("title", request.getTitle());
        data.put("body", request.getBody());
        data.put("userId", request.getUserId().toString());
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        // 커스텀 데이터 추가
        if (request.getData() != null) {
            data.putAll(request.getData());
        }

        // 액션 데이터 (Flutter에서 처리용)
        if (request.getData() != null) {
            String notificationType = request.getData().get("notificationType");
            if (notificationType != null) {
                data.put("actionType", getActionType(notificationType));
                data.put("actionData", getActionData(request.getData()));
            }
        }

        return data;
    }

    private Map<String, String> createBatchNotificationData(NotificationDto.BatchSendRequest request) {
        Map<String, String> data = new HashMap<>();

        data.put("type", "batch_notification");
        data.put("title", request.getTitle());
        data.put("body", request.getBody());
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        if (request.getData() != null) {
            data.putAll(request.getData());
        }

        return data;
    }

    private String getActionType(String notificationType) {
        return switch (notificationType) {
            case "COMMENT", "LIKE" -> "NAVIGATE";
            case "MARKETING", "ANNOUNCEMENT" -> "OPEN_URL";
            default -> "NONE";
        };
    }

    private String getActionData(Map<String, String> data) {
        String postId = data.get("postId");
        String commentId = data.get("commentId");

        if (postId != null) {
            return "/post/" + postId;
        } else if (commentId != null) {
            return "/comment/" + commentId;
        }

        return "";
    }

    private void handleFCMError(FirebaseMessagingException e, Long userId) {
        String errorCode = e.getMessagingErrorCode().name();

        switch (errorCode) {
            case "UNREGISTERED":
            case "INVALID_REGISTRATION_TOKEN":
                // FCM 토큰이 유효하지 않음 - DB에서 제거
                log.warn("Invalid FCM token for user {}, removing from database", userId);
                userRepository.clearFcmToken(userId);
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

    private void handleBatchErrors(BatchResponse response, List<String> tokens) {
        if (response.getFailureCount() > 0) {
            List<SendResponse> responses = response.getResponses();

            for (int i = 0; i < responses.size(); i++) {
                SendResponse sendResponse = responses.get(i);
                if (!sendResponse.isSuccessful()) {
                    String token = tokens.get(i);
                    FirebaseMessagingException exception = sendResponse.getException();

                    if (exception != null) {
                        String errorCode = exception.getMessagingErrorCode().name();
                        if ("UNREGISTERED".equals(errorCode) || "INVALID_REGISTRATION_TOKEN".equals(errorCode)) {
                            // 유효하지 않은 토큰 처리
                            userRepository.clearFcmTokenByToken(token);
                            log.warn("Removed invalid FCM token: {}", token);
                        }
                    }
                }
            }
        }
    }

    /**
     * FCM 토큰 업데이트
     */
    public void updateFcmToken(Long userId, String fcmToken) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

            user.setFcmToken(fcmToken);
            userRepository.save(user);

            log.info("FCM token updated for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to update FCM token for user {}", userId, e);
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

    public boolean registerToken(NotificationDto.TokenRegistrationRequest request, Long id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

            // 기존 토큰이 있다면 제거
            if (user.getFcmToken() != null) {
                userRepository.clearFcmToken(id);
            }

            // 새 토큰 저장
            user.setFcmToken(request.getFcmToken());
            userRepository.save(user);

            log.info("FCM token registered for user: {}", id);
            return true;
        } catch (Exception e) {
            log.error("Failed to register FCM token for user {}", id, e);
            return false;
        }
    }
}