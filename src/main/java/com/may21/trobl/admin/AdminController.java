package com.may21.trobl.admin;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
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

    @GetMapping("/reported-posts")
    public ResponseEntity<Message> getReportedPosts(@RequestHeader("Authorization") String token) {
        JwtTokenUtil.getAdminUserByToken(token);
        List<PostDto.ListItem> response = postingService.getAllReportedPosts();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/reported-posts/{postId}/block")
    public ResponseEntity<Message> unblockPost(@RequestHeader("Authorization") String token,
                                               @PathVariable Long postId) {
        JwtTokenUtil.getAdminUserByToken(token);
        boolean response = postingService.unblockPost(postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/reported-posts/{postId}")
    public ResponseEntity<Message> deletePost(@RequestHeader("Authorization") String token,
                                              @PathVariable Long postId) {
        JwtTokenUtil.getAdminUserByToken(token);
        boolean response = postingService.deletePostByAdmin(postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @PostMapping("/notifications/send")
    public ResponseEntity<Message> sendNotification(@RequestHeader("Authorization") String token,
                                                    @RequestBody AdminDto.PushNotification message) {
        JwtTokenUtil.getAdminUserByToken(token);
        boolean response = notificationService.notifyMarketingAlert(message);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
