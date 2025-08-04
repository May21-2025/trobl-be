package com.may21.trobl.post.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.redis.CacheService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/fair-view")
public class FairViewController {
    private final PostingService postingService;
    private final JwtTokenUtil jwtTokenUtil;
    private final NotificationService notificationService;
    private final CacheService cacheService;

    @PutMapping("/{fairViewId}")
    public ResponseEntity<Message> addFairView(
            @PathVariable Long fairViewId,
            @RequestBody PostDto.FairViewRequest request, @RequestHeader("Authorization") String token) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token).getId();
        PostDto.FairViewItem response = postingService.setFairView(fairViewId, userId, request);
        cacheService.evictFairViewFromCache(fairViewId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/{fairViewId}/confirm")
    public ResponseEntity<Message> confirmFairView(
            @PathVariable Long fairViewId,  @RequestHeader("Authorization") String token)  {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        Long userId = user != null ? user.getId() : null;
        boolean response = postingService.confirmFairView(userId, fairViewId);
        if(response) {
            notificationService.sendFairViewConfirmedRequest(fairViewId);
        }
        cacheService.evictFairViewFromCache(fairViewId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/confirm")
    public ResponseEntity<Message> getFairViewConfirmList(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (user == null) throw new BusinessException(ExceptionCode.TOKEN_MISSING);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostDto.RequestedItem>
                response = postingService.getFairViewConfirmList(user.getId(), pageable);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/requested")
    public ResponseEntity<Message> getFairViewRequestedList(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = jwtTokenUtil.getUserFromValidateAccessToken(token);
        if (user == null) throw new BusinessException(ExceptionCode.TOKEN_MISSING);
        Page<PostDto.RequestedListItem> response = postingService.getFairViewRequestedList(user.getId(),
                page,size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
