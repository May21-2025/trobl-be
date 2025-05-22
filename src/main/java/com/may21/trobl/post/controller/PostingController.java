package com.may21.trobl.post.controller;

import com.may21.trobl._global.Message;
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
  public ResponseEntity<Message> getAllPostsList(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    // PageRequest 객체 생성 (페이지, 사이즈, 정렬 정보)
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<PostDto.ListItem> response = postingService.getPostsList(pageable);
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }

  @GetMapping("/top5")
  public ResponseEntity<Message> getPostsView() {
    List<PostDto.View> response = postingService.getTop5Views();
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }


  @GetMapping("/top10")
  public ResponseEntity<Message> getTop10PostsView(@RequestParam String type) {
    List<PostDto.ListItem> response = postingService.getTop10Views(type);
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
  public ResponseEntity<Message> getPostDetail(@PathVariable Long postId) {
    PostDto.Detail response = postingService.getPostDetail(postId);
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }
  @DeleteMapping("/{postId}")
  public ResponseEntity<Message> deletePost(
          @PathVariable Long postId,
          @AuthenticationPrincipal User user) {
    boolean response = postingService.deletePost(user.getId(), postId);
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }
  @PutMapping("/{postId}/like")
  public ResponseEntity<Message> likePost(
      @PathVariable Long postId, @AuthenticationPrincipal User user) {
    boolean response = postingService.likePost(postId, user.getId());
    if(response) notificationService.sendPostLikeNotification(postId, user.getId());
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
}
