package com.may21.trobl.comment.service;

import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.comment.domain.CommentLike;
import com.may21.trobl.comment.domain.CommentLikeRepository;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
  private final CommentRepository commentRepository;
  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final CommentLikeRepository likeRepository;

  @Override
  public List<CommentDto.Response> getComments(Long postId, Long userId) {
    List<Comment> comments = commentRepository.findByPostId(postId);
    List<Long> userIds = comments.stream().map(Comment::getUserId).distinct().toList();
    List<User> users = userRepository.findByIdIn(userIds);
    Map<Long, User> userMap =
            users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    List<CommentLike> likes = likeRepository.findByUserIdAndInComments(userId, comments);
    List<Comment> likedList = likes.stream().map(CommentLike::getComment).toList();
    return comments.stream()
            .map(
                    comment ->
                            new CommentDto.Response(
                                    comment, userMap.get(comment.getUserId()), likedList.contains(comment)))
            .toList();
  }

  @Override
  public CommentDto.Response createComment(Long postId, CommentDto.Request request, Long userId) {
    Posting post =
            postRepository
                    .findById(postId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
    Comment parentComment = request.getCommentId() == null ? null :
            commentRepository
                    .findById(request.getCommentId())
                    .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));

    Comment comment = new Comment(user, post, parentComment, request.getContent());
    commentRepository.save(comment);
    return new CommentDto.Response(comment, user, false);
  }

  @Override
  public CommentDto.Response updateComment(
          Long userId, CommentDto.Request request, Long commentId) {
    Comment comment =
            commentRepository
                    .findById(commentId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
    if (!comment.getUserId().equals(userId)) {
      throw new BusinessException(ExceptionCode.UNAUTHORIZED);
    }
    comment.setContent(request.getContent());
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

    CommentLike like = likeRepository.findByUserIdAndComment(userId, comment);
    return new CommentDto.Response(comment, user, like != null);
  }

  @Override
  public boolean likeComment(Long commentId, Long userId) {
    boolean liked = true;
    CommentLike commentLike =
            likeRepository.findByCommentIdAndUserId(commentId, userId).orElse(null);
    if (commentLike == null) {
      Comment comment =
              commentRepository
                      .findById(commentId)
                      .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
      commentLike = new CommentLike(comment, userId);
      likeRepository.save(commentLike);
    } else {
      liked = false;
      likeRepository.deleteByEntity(commentLike);
    }
    return liked;
  }

  @Override
  public boolean deleteComment(Long userId, Long commentId) {
    Comment comment =
            commentRepository
                    .findById(commentId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
    if (!comment.getUserId().equals(userId)) {
      throw new BusinessException(ExceptionCode.UNAUTHORIZED);
    }
    commentRepository.delete(comment);
    return true;
  }

  @Override
  public Page<CommentDto.RecentInfo> getMyComments(Long userId, int page, int size) {
    Page<Comment> comments = commentRepository.findByUserId(userId, PageRequest.of(page, size));
    User user =
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
    List<CommentLike> likes = likeRepository.findByUserIdAndInComments(userId, comments.stream().toList());
    List<Comment> likedList = likes.stream().map(CommentLike::getComment).toList();
    return comments.map(
            comment ->
                    new CommentDto.RecentInfo(
                            comment.getPosting(),
                            comment,
                            user,
                            likedList.contains(comment)));
  }

  @Override
  public Map<Long, Integer> getPostCommentMap(List<Posting> posts) {
    Map<Long, Integer> postCommentMap = new HashMap<>();

    if (posts == null || posts.isEmpty()) {
      return postCommentMap;
    }

    List<Long> postIds = posts.stream()
            .map(Posting::getId)
            .toList();

    List<Comment> comments = commentRepository.findByPostIdIn(postIds);

    if (comments != null && !comments.isEmpty()) {
      postCommentMap = comments.stream()
              .collect(Collectors.toMap(
                      c -> c.getPosting().getId(),
                      c -> 1,
                      Integer::sum
              ));
    }

    // Ensure every post ID is in the map, even those with 0 comments
    for (Posting post : posts) {
      postCommentMap.putIfAbsent(post.getId(), 0);
    }

    return postCommentMap;
  }
}