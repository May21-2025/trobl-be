package com.may21.trobl.notification.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.pushAlarm.PushNotificationService;
import com.may21.trobl.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final PushNotificationService webPushService;

    @PostMapping("/register-web-push-token")
    public ResponseEntity<Message> registerToken(
            @RequestBody NotificationDto.TokenRegistrationRequest request,
            @AuthenticationPrincipal User user) {
        boolean response = webPushService.registerToken(request, user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Message> markNotificationAsRead(
            @PathVariable Long notificationId, @AuthenticationPrincipal User user) {
        boolean response = notificationService.markAsRead(notificationId, user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/settings")
    public ResponseEntity<Message> setNotificationSettings(
            @RequestParam Boolean enabled,
            @RequestParam String notificationType, @AuthenticationPrincipal User user) {
        boolean response = notificationService.setNotificationSettings(user.getId(), notificationType, enabled);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


}
