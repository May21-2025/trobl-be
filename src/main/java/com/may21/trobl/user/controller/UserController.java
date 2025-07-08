package com.may21.trobl.user.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.auth.service.AuthorizationService;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.comment.service.CommentService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.report.ReportDto;
import com.may21.trobl.storage.StorageService;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final PostingService postingService;
    private final CommentService commentService;
    private final AuthorizationService authorizationService;
    private final StorageService storageService;


    @GetMapping("/bookmarks")
    public ResponseEntity<Message> getBookmarkedPosts(@AuthenticationPrincipal User user,
                                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<PostDto.ListItem> response = postingService.getBookmarkedPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @PatchMapping("/bookmarks")
    public ResponseEntity<Message> updateNotificationSetting(@AuthenticationPrincipal User user, @RequestParam String type, @RequestParam boolean enabled) {
        UserDto.NotificationSetting response = userService.updateNotificationSetting(user.getId(), type, enabled);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/comments")
    public ResponseEntity<Message> getMyComments(@AuthenticationPrincipal User user,
                                                 @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<CommentDto.RecentInfo> response = commentService.getMyComments(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    // EMAIL ALARM SETTING
    @GetMapping("/email-alarm")
    public ResponseEntity<Message> getEmailSettings(@AuthenticationPrincipal User user) {
        UserDto.AlertSetting response = userService.getEmailAlarmStatus(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/email-alarm")
    public ResponseEntity<Message> setEmailAlarmStatus(
            @RequestBody UserDto.AlertSetting request, @AuthenticationPrincipal User user) {
        userService.setEmailAlarmStatus(request, user.getId());
        return new ResponseEntity<>(Message.success(null), HttpStatus.OK);
    }

    @GetMapping("/info")
    public ResponseEntity<Message> getUserData(@AuthenticationPrincipal User user) {
        UserDto.Info response = userService.getUserData(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @GetMapping("/likes")
    public ResponseEntity<Message> getLikedPosts(@AuthenticationPrincipal User user,
                                                 @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<PostDto.ListItem> response = postingService.getLikedPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/marriage-info")
    public ResponseEntity<Message> updateInformation(@AuthenticationPrincipal User user, @RequestBody UserDto.MarriedInfo requestBody) {
        boolean response = userService.updateInformation(user.getId(), requestBody);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<Message> updateNickname(@AuthenticationPrincipal User user, @RequestParam String nickname) {
        boolean response = userService.updateNickname(user.getId(), nickname);
        postingService.setNickname(user.getId(), nickname);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/notifications")
    public ResponseEntity<Message> getUsersNotification(@AuthenticationPrincipal User user) {
        UserDto.NotificationSetting response = userService.getUsersNotification(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/profiles")
    public ResponseEntity<Message> getUserProfile(@AuthenticationPrincipal User user) {
        UserDto.InfoDetail response = userService.getUserInfoDetail(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/profiles")
    public ResponseEntity<Message> updateUserProfileImage(@AuthenticationPrincipal User user, @RequestParam(required = false) MultipartFile image) {
        String imageKey = storageService.uploadUserProfileImage(user.getId(), image);
        UserDto.Info response = userService.updateUserProfileImage(user.getId(),imageKey);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/posts")
    public ResponseEntity<Message> getMyPosts(@AuthenticationPrincipal User user,
                                              @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<PostDto.ListItem> response = postingService.getMyPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/partner")
    public ResponseEntity<Message> matchPartner(@AuthenticationPrincipal User user, @RequestParam String partnerId) {
        boolean response = userService.matchPartner(user.getId(), partnerId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/visits")
    public ResponseEntity<Message> getVisitedPosts(@AuthenticationPrincipal User user,
                                                   @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<PostDto.ListItem> response = postingService.getVisitedPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/voted")
    public ResponseEntity<Message> getVotedPosts(@AuthenticationPrincipal User user,
                                                 @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        Page<PostDto.ListItem> response = postingService.getVotedPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/unregister")
    public ResponseEntity<Message> unregister(@AuthenticationPrincipal User user) {
        boolean response = authorizationService.unregister(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{userId}/report")
    public ResponseEntity<Message> reportPost(
            @PathVariable Long userId,
            @RequestBody ReportDto.Request reportRequest,
            @AuthenticationPrincipal User user) {
        boolean response = userService.reportUser(user.getId(), userId,reportRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
