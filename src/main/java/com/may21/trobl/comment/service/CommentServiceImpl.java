package com.may21.trobl.comment.service;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.utility.ProfanityFilter;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.comment.domain.CommentLike;
import com.may21.trobl.comment.domain.CommentLikeRepository;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.notification.domain.ContentUpdateService;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.redis.CacheService;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.report.ReportDto;
import com.may21.trobl.report.ReportService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentLikeRepository likeRepository;
    private final ReportService reportService;
    private final ContentUpdateService contentUpdateService;
    private final ProfanityFilter profanityFilter;
    private final CacheService cacheService;

    @Override
    public List<CommentDto.Response> getComments(Long postId, Long userId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        List<Comment> filteredComments = reportService.filterBlockedComments(userId, comments);
        List<Long> userIds = filteredComments.stream()
                .map(Comment::getUserId)
                .distinct()
                .toList();
        List<User> users = userRepository.findByIdIn(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<CommentLike> likes =
                likeRepository.findByUserIdAndInComments(userId, filteredComments);
        List<Comment> likedList = likes.stream()
                .map(CommentLike::getComment)
                .toList();
        return filteredComments.stream()
                .map(comment -> new CommentDto.Response(comment, userMap.get(comment.getUserId()),
                        likedList.contains(comment)))
                .toList();
    }

    @Override
    public CommentDto.Response createComment(Long postId, CommentDto.Request request, Long userId) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        Comment parentComment = request.getCommentId() == null ? null :
                commentRepository.findById(request.getCommentId())
                        .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
        Comment comment = new Comment(user, post, parentComment,
                profanityFilter.filterProfanity(request.getContent()));
        commentRepository.save(comment);
        return new CommentDto.Response(comment, user, false);
    }

    @Override
    public CommentDto.Response updateComment(Long userId, CommentDto.Request request,
            Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
        if (!comment.getUserId()
                .equals(userId)) {
            throw new BusinessException(ExceptionCode.UNAUTHORIZED);
        }
        comment.setContent(profanityFilter.filterProfanity(request.getContent()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        CommentLike like = likeRepository.findByUserIdAndComment(userId, comment);
        return new CommentDto.Response(comment, user, like != null);
    }

    @Override
    public CommentDto.Response likeComment(Long commentId, Long userId) {
        boolean liked = true;
        CommentLike commentLike = likeRepository.findByCommentIdAndUserId(commentId, userId)
                .orElse(null);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
        if (commentLike == null) {
            commentLike = new CommentLike(comment, userId);
            likeRepository.save(commentLike);
        }
        else {
            liked = false;
            likeRepository.deleteByEntity(commentLike);
        }
        User commentOwner = userRepository.findById(comment.getUserId())
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        return new CommentDto.Response(comment, commentOwner, liked);
    }

    @Override
    public boolean deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
        if (!comment.getUserId()
                .equals(userId)) {
            throw new BusinessException(ExceptionCode.UNAUTHORIZED);
        }
        commentRepository.delete(comment);
        return true;
    }

    @Override
    public Page<CommentDto.MyComments> getMyComments(Long userId, int page, int size) {
        Page<Comment> comments = commentRepository.findByUserId(userId, PageRequest.of(page, size));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        List<Comment> commentList = comments.stream()
                .toList();
        List<CommentLike> likes = likeRepository.findByUserIdAndInComments(userId, commentList);
        List<Comment> likedList = likes.stream()
                .map(CommentLike::getComment)
                .toList();
        Map<Long, NotificationDto.ContentUpdateStatus> contentUpdates =
                contentUpdateService.getContentUpdatesByUserId(userId, commentList, Comment::getId,
                        ItemType.COMMENT);

        return comments.map(
                comment -> new CommentDto.MyComments(comment.getPosting(), comment, user,
                        likedList.contains(comment), contentUpdates.get(comment.getId())));
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
                    .collect(Collectors.toMap(c -> c.getPosting()
                            .getId(), c -> 1, Integer::sum));
        }

        // Ensure every post ID is in the map, even those with 0 comments
        for (Posting post : posts) {
            postCommentMap.putIfAbsent(post.getId(), 0);
        }

        return postCommentMap;
    }

    @Override
    public boolean existsByPostIdAndUserId(Long postId, Long userId) {
        return commentRepository.existsByPostingIdAndUserId(postId, userId);
    }

    @Override
    public boolean reportComment(Long userId, Long commentId, ReportDto.Request reportRequest) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));

        int reportedCount =
                reportService.report(userId, commentId, ItemType.COMMENT, reportRequest);
        if (reportedCount >= 10) {
            comment.setReported(true);
        }
        return true;
    }

    @Override
    public CommentDto.Response createVirtualComment(AdminDto.VirtualCommentRequest createRequest) {
        Long postId = createRequest.getPostId();
        Long userId = createRequest.getUserId();
        Long commentId = createRequest.getCommentId();
        String content = profanityFilter.filterProfanity(createRequest.getContent());
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        Comment parentComment = commentId == null ? null : commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
        Comment comment = new Comment(user, post, parentComment, content);
        commentRepository.save(comment);
        return new CommentDto.Response(comment, user, false);
    }

    @Override
    public boolean deleteVirtualComments(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
        commentRepository.delete(comment);
        return true;
    }

    @Override
    public List<AdminDto.CommentInfo> getAdminCommentInfo(Long postId) {
        List<Comment> comments = commentRepository.findByPostId(postId);
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .distinct()
                .toList();
        List<User> users = userRepository.findByIdIn(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return comments.stream()
                .map(comment -> new AdminDto.CommentInfo(comment, userMap.get(comment.getUserId())))
                .toList();
    }

    @Override
    public CommentDto.Response updateVirtualComments(Long commentId,
            AdminDto.VirtualCommentRequest createRequest) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
        comment.setContent(profanityFilter.filterProfanity(createRequest.getContent()));
        commentRepository.save(comment);
        RedisDto.UserDto userDto = cacheService.getUserFromCache(comment.getUserId());
        return new CommentDto.Response(comment, userDto, false);
    }
}