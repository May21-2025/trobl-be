package com.may21.trobl.post.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.user.domain.User;
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
    public ResponseEntity<Message> getRandomQuickPoll() {
        List<PostDto.QuickPoll> response = postingService.getRandomQuickPoll();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/top-list")
    public ResponseEntity<Message> getTopListPostsView(@RequestParam String type, @RequestParam(required = false, defaultValue = "10") int count) {
        List<PostDto.Card> response = postingService.getTop10Views(type);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @PostMapping("")
    public ResponseEntity<Message> createPost(
            @RequestBody PostDto.Request request, @AuthenticationPrincipal User user) {
        PostDto.Detail response = postingService.createPost(request, user.getId());
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
    public ResponseEntity<Message> getPostDetail(@PathVariable Long postId, @AuthenticationPrincipal User user) {
        PostDto.Detail response = postingService.getPostDetail(postId, user);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Message> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal User user) {
        boolean response = postingService.deletePost(user.getId(), postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/{postId}/fair-view")
    public ResponseEntity<Message> addPairView(
            @PathVariable Long postId,
            @RequestBody PostDto.OpinionItem opinionItem, @AuthenticationPrincipal User user) {
        PostDto.Detail response = postingService.addPairView(postId, user.getId(), opinionItem);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/{postId}/fair-view/{opinionId}")
    public ResponseEntity<Message> confirmFairView(
            @PathVariable Long postId,
            @PathVariable Long opinionId, @AuthenticationPrincipal User user) {
        boolean response = postingService.confirmFairView(user.getId(), opinionId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{postId}/like")
    public ResponseEntity<Message> likePost(
            @PathVariable Long postId, @AuthenticationPrincipal User user) {
        boolean response = postingService.likePost(postId, user.getId());
        if (response) notificationService.sendPostLikeNotification(postId, user.getId());
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

    @GetMapping("/fair-view/confirm")
    public ResponseEntity<Message> getFairViewConfirmList(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (user == null) throw new BusinessException(ExceptionCode.TOKEN_MISSING);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostDto.ListItem> response = postingService.getFairViewConfirmList(user.getId(), pageable);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/fair-view/confirm/{postId}")
    public ResponseEntity<Message> confirmFairViewPost(
            @AuthenticationPrincipal User user, @PathVariable Long postId) {
        if (user == null) throw new BusinessException(ExceptionCode.TOKEN_MISSING);
        boolean response = postingService.confirmFairViewPost(user.getId(), postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<Message> search(
            @AuthenticationPrincipal User user, @RequestParam String keyword) {
        Long userId = user != null ? user.getId() : null;
        List<PostDto.ListItem> response = postingService.searchPostsByKeyword(userId, keyword);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
