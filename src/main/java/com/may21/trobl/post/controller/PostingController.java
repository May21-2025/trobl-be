package com.may21.trobl.post.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.notification.domain.ContentUpdateService;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.report.ReportDto;
import com.may21.trobl.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/postings")
public class PostingController {

    private final PostingService postingService;
    private final NotificationService notificationService;
    private final JwtTokenUtil jwtTokenUtil;
    private final ContentUpdateService contentUpdateService;


    @PostMapping("")
    public ResponseEntity<Message> createPost(
            @RequestBody PostDto.Request request, @RequestHeader("Authorization") String token) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token).getId();
        PostDto.Detail response = postingService.createPost(request, userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Message> updatePost(
            @RequestBody PostDto.Request request,
            @PathVariable Long postId,
            @AuthenticationPrincipal User user) {
        PostDto.Detail response = postingService.updatePost(request, user.getId(), postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Message> getPostDetail(@PathVariable Long postId, @AuthenticationPrincipal User user, HttpServletRequest request) {
        if (user == null) {
            String authHeader = request.getHeader("Authorization");
        }
        Long userId = user != null ? user.getId() : null;
        PostDto.Detail response = postingService.getPostDetail(postId, userId);
        contentUpdateService.readIfExist(userId, postId, ItemType.POST);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Message> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user) {
        boolean response = postingService.deletePost(user.getId(), postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{postId}/report")
    public ResponseEntity<Message> reportPost(
            @PathVariable Long postId,
            @RequestBody ReportDto.Request reportRequest,
            @AuthenticationPrincipal User user) {
        boolean response = postingService.reportPost(user.getId(), postId, reportRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{postId}/like")
    public ResponseEntity<Message> likePost(
            @PathVariable Long postId, @AuthenticationPrincipal User user) {
        PostDto.ListItem response = postingService.likePost(postId, user.getId());
        if (response.isLiked()) {
            notificationService.sendPostLikeNotification(postId, user.getId());
            contentUpdateService.likeUpdate(postId, ItemType.POST);
        }
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{postId}/bookmarks")
    public ResponseEntity<Message> bookmarkPost(
            @PathVariable Long postId, @AuthenticationPrincipal User user) {
        boolean response = postingService.bookmarkPost(postId, user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/{postId}/share")
    public ResponseEntity<Message> sharePost(
            @PathVariable Long postId, @AuthenticationPrincipal User user) {
        boolean response = postingService.sharePost(postId, user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/{postId}/view")
    public ResponseEntity<Message> viewPost(
            @PathVariable Long postId, @AuthenticationPrincipal User user) {
        boolean response = postingService.viewPost(postId, user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @PatchMapping("/{postId}/confirm")
    public ResponseEntity<Message> confirmFairViewPost(
            @RequestHeader("Authorization") String token, @PathVariable Long postId) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token).getId();
        boolean response = postingService.confirmFairViewPost(userId, postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @GetMapping("/all")
    public ResponseEntity<Message> getAllPostsList(@AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        // PageRequest 객체 생성 (페이지, 사이즈, 정렬 정보)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Long userId = user != null ? user.getId() : null;
        Page<PostDto.ListItem> response = postingService.getPostsList(pageable, userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/quick-poll")
    public ResponseEntity<Message> getRandomQuickPoll(@AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        List<PostDto.QuickPoll> response = postingService.getRandomQuickPoll(userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/fair-view")
    public ResponseEntity<Message> getFairView(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
            Page<PostDto.ListItem> response = postingService.getFairViewList(userId, page, size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/top-list")
    public ResponseEntity<Message> getTopListPostsView(@AuthenticationPrincipal User user, @RequestParam(required = false, defaultValue = "all") String type, @RequestParam(required = false, defaultValue = "10") int count) {
        Long userId = user != null ? user.getId() : null;
        List<PostDto.Card> response = postingService.getTop10Views(type, userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }



    @GetMapping("/search")
    public ResponseEntity<Message> search(
            @AuthenticationPrincipal User user, @RequestParam String keyword) {
        Long userId = user != null ? user.getId() : null;
        List<PostDto.ListItem> response = postingService.searchPostsByKeyword(userId, keyword);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/cache-reset")
    public ResponseEntity<Message> resetCache() {
        postingService.evictAllTopPosts();
        return new ResponseEntity<>(Message.success(true), HttpStatus.OK);
    }

}
