package com.may21.trobl.user.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.auth.service.AuthorizationService;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.comment.service.CommentService;
import com.may21.trobl.partner.PartnerService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final PostingService postingService;
    private final CommentService commentService;
    private final AuthorizationService authorizationService;
    private final StorageService storageService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PartnerService partnerService;


    @GetMapping("/bookmarks")
    public ResponseEntity<Message> getBookmarkedPosts(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        Page<PostDto.ListItem> response = postingService.getBookmarkedPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @PatchMapping("/bookmarks")
    public ResponseEntity<Message> updateNotificationSetting(
            @RequestHeader("Authorization") String token, @RequestParam String type, @RequestParam boolean enabled) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        UserDto.NotificationSetting response = userService.updateNotificationSetting(user.getId(), type, enabled);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/comments")
    public ResponseEntity<Message> getMyComments(@RequestHeader("Authorization") String token,
                                                 @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        Page<CommentDto.MyComments> response = commentService.getMyComments(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    // EMAIL ALARM SETTING
    @GetMapping("/email-alarm")
    public ResponseEntity<Message> getEmailSettings(@RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        UserDto.AlertSetting response = userService.getEmailAlarmStatus(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/email-alarm")
    public ResponseEntity<Message> setEmailAlarmStatus(
            @RequestBody UserDto.AlertSetting request, @RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        userService.setEmailAlarmStatus(request, user.getId());
        return new ResponseEntity<>(Message.success(null), HttpStatus.OK);
    }

    @GetMapping("/info")
    public ResponseEntity<Message> getUserData(@RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        UserDto.Info response = userService.getUserData(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @GetMapping("/likes")
    public ResponseEntity<Message> getLikedPosts(@RequestHeader("Authorization") String token,
                                                 @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        Page<PostDto.ListItem> response = postingService.getLikedPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/marriage-info")
    public ResponseEntity<Message> updateInformation(@RequestHeader("Authorization") String token, @RequestBody UserDto.MarriedInfo requestBody) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        boolean response = userService.updateInformation(user.getId(), requestBody);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<Message> check(@RequestHeader("Authorization") String token, @RequestParam String nickname) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        boolean response = userService.updateNickname(user.getId(), nickname);
        postingService.setNickname(user.getId(), nickname);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/notifications")
    public ResponseEntity<Message> getUsersNotification(@RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        UserDto.NotificationSetting response = userService.getUsersNotification(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/profiles")
    public ResponseEntity<Message> getUserProfile(@RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        UserDto.InfoDetail response = userService.getUserInfoDetail(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/profiles")
    public ResponseEntity<Message> updateUserProfileImage(@RequestHeader("Authorization") String token, @RequestParam(required = false) MultipartFile image) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        String imageKey = storageService.uploadUserProfileImage(user.getId(), image);
        UserDto.Info response = userService.updateUserProfileImage(user.getId(), imageKey);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/posts")
    public ResponseEntity<Message> getMyPosts(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        Page<PostDto.MyListItem> response = postingService.getMyPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/partner-requests")
    public ResponseEntity<Message> requestPartner(@RequestHeader("Authorization") String token,
                                                  @RequestBody UserDto.RequestPartner request) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token).getId();
        boolean response = partnerService.requestPartner(userId, request);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @GetMapping("/partner-requests/{partnerRequestId}")
    public ResponseEntity<Message> checkMarriageDate(@RequestHeader("Authorization") String token,
            @PathVariable Long partnerRequestId,
            @RequestParam LocalDate marriageDate) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token).getId();
        boolean response = partnerService.checkMarriageDate(partnerRequestId, marriageDate);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @DeleteMapping("/partner-requests/{partnerRequestId}")
    public ResponseEntity<Message> deleteMarriageInfo(@RequestHeader("Authorization") String token,
            @PathVariable Long partnerRequestId) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token).getId();
        boolean response = partnerService.deleteMarriageInfo(partnerRequestId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @PatchMapping("/partner-requests/{partnerRequestId}")
    public ResponseEntity<Message> responseToRequest(@RequestHeader("Authorization") String token,
                                                     @PathVariable Long partnerRequestId,
                                                     @RequestBody UserDto.AcceptPartnerRequest request) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token).getId();
        boolean response = partnerService.matchPartner(userId, partnerRequestId, request);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/partner-requests")
    public ResponseEntity<Message> getPartnerRequest(@RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        UserDto.PartnerInfo response = partnerService.getRequestStatus(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @GetMapping("/visits")
    public ResponseEntity<Message> getVisitedPosts(@RequestHeader("Authorization") String token,
                                                   @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        Page<PostDto.ListItem> response = postingService.getVisitedPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/voted")
    public ResponseEntity<Message> getVotedPosts(@RequestHeader("Authorization") String token,
                                                 @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        Page<PostDto.ListItem> response = postingService.getVotedPosts(user.getId(), page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/unregister")
    public ResponseEntity<Message> unregister(@RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        boolean response = authorizationService.unregister(user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{userId}/report")
    public ResponseEntity<Message> reportPost(
            @PathVariable Long userId,
            @RequestBody ReportDto.Request reportRequest,
            @RequestHeader("Authorization") String token) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        boolean response = userService.reportUser(user.getId(), userId, reportRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<Message> deleteAllOAuth() {
        boolean response = userService.deleteAllOauth();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
