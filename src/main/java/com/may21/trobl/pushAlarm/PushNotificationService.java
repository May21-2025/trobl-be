package com.may21.trobl.pushAlarm;

import com.google.firebase.messaging.*;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.notification.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {
    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    public String sendNotification(String token, String title, String body, Map<String, String> data)
            throws FirebaseMessagingException {
        Notification notification = Notification.builder().setTitle(title).setBody(body).build();

        Message message =
                Message.builder()
                        .setToken(token)
                        .setNotification(notification)
                        .putAllData(data)
                        .build();

        return firebaseMessaging.send(message);
    }

    public BatchResponse sendMulticastNotification(
            List<String> tokens, String title, String body, Map<String, String> data)
            throws FirebaseMessagingException {

        MulticastMessage message =
                MulticastMessage.builder()
                        .addAllTokens(tokens)
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putAllData(data)
                        .build();

        return firebaseMessaging.sendEachForMulticast(message);
    }

    public void sendNotificationInChunks(List<String> tokens, String title, String body, Map<String, String> data) {
        int batchSize = 500;
        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> chunk = tokens.subList(i, Math.min(tokens.size(), i + batchSize));
            try {
                sendMulticastNotification(chunk, title, body, data);
            } catch (FirebaseMessagingException e) {
                log.error("Failed to send notification chunk", e);
            }
        }
    }

    public boolean sendNotificationTo(NotificationDto.SendRequest request) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(request.getUserId());
        if (tokens.isEmpty()) return false;

        List<String> deviceTokens = tokens.stream().map(DeviceToken::getDeviceToken).toList();
        try {
            BatchResponse response =
                    sendMulticastNotification(
                            deviceTokens, request.getTitle(), request.getBody(), request.getData());
            log.info(
                    "Notification sent: {} successful, {} failed",
                    response.getSuccessCount(),
                    response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            throw new BusinessException(ExceptionCode.FORBIDDEN);
        }
        return true;
    }

    @Transactional
    public boolean registerToken(NotificationDto.TokenRegistrationRequest request, Long userId) {
        DeviceToken token = new DeviceToken();
        token.setUserId(userId);
        token.setDeviceToken(request.getDeviceToken());
        deviceTokenRepository.save(token);
        return true;
    }
}
