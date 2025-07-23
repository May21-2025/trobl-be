package com.may21.trobl.admin;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.auth.jwt.TokenInfo;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.pushAlarm.PushNotificationService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final PostingService postingService;
    private final NotificationService notificationService;
    private final PushNotificationService  pushNotificationService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;
    private final AdminService adminService;

    @GetMapping("/authenticate")
    public ResponseEntity<Message> authenticateAdmin(@RequestHeader("Authorization") String token) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token).getId();
        boolean response  = adminService.grantAdminRole(userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/reported-posts")
    public ResponseEntity<Message> getReportedPosts(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<PostDto.ListItem> response = postingService.getAllReportedPosts();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/user-token/{userId}")
    public ResponseEntity<Message> getUserToken(@RequestHeader("Authorization") String token,
                                                @PathVariable Long userId,
                                                HttpServletResponse response) {
        jwtTokenUtil.getAdminUserByToken(token);
        User user = userService.getUser(userId);
        TokenInfo userToken =
                jwtTokenUtil.generateAccessAndRefreshToken(user, null, null, "unknown");
        userToken.tokenToHeaders(response);
        return new ResponseEntity<>(Message.success(true), HttpStatus.OK);
    }

    @PatchMapping("/reported-posts/{postId}/block")
    public ResponseEntity<Message> unblockPost(@RequestHeader("Authorization") String token,
                                               @PathVariable Long postId) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = postingService.unblockPost(postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/reported-posts/{postId}")
    public ResponseEntity<Message> deletePost(@RequestHeader("Authorization") String token,
                                              @PathVariable Long postId) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = postingService.deletePostByAdmin(postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @PostMapping("/notifications/send")
    public ResponseEntity<Message> sendNotification(@RequestHeader("Authorization") String token,
                                                    @RequestBody AdminDto.PushNotification message) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = notificationService.notifyMarketingAlert(message);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


}
