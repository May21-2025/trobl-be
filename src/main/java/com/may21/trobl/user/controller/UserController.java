package com.may21.trobl.user.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.comment.service.CommentService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final PostingService postingService;
  private final CommentService commentService;

  @GetMapping("/info")
  public ResponseEntity<Message> getUserData( @AuthenticationPrincipal User user) {
    UserDto.Info response = userService.getUserData(user.getId());
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }


  @GetMapping("/profiles")
  public ResponseEntity<Message> getUserProfile(@AuthenticationPrincipal User user) {
    UserDto.InfoDetail response = userService.getUserInfoDetail(user.getId());
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }

  @PutMapping("/profiles")
  public ResponseEntity<Message> updateUserProfile(
      @RequestBody UserDto.InfoRequest userRequestDto,
      @AuthenticationPrincipal User user) {
    UserDto.Info response = userService.updateUserProfile(userRequestDto, user.getId());
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

  @GetMapping("/posts")
  public ResponseEntity<Message> getMyPosts(@AuthenticationPrincipal User user) {
    List<PostDto.ListItem> response = postingService.getMyPosts(user.getId());
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }
  @GetMapping("/comments")
  public ResponseEntity<Message> getMyComments(@AuthenticationPrincipal User user) {
    List<CommentDto.RecentInfo> response = commentService.getMyComments(user.getId());
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }
  @GetMapping("/likes")
  public ResponseEntity<Message> getLikedPosts(@AuthenticationPrincipal User user) {
    List<PostDto.ListItem> response = postingService.getLikedPosts(user.getId());
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }
  @GetMapping("/visits")
  public ResponseEntity<Message> getVisitedPosts(@AuthenticationPrincipal User user) {
    List<PostDto.ListItem> response = postingService.getVisitedPosts(user.getId());
    return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
  }

}
