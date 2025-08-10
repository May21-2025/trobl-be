package com.may21.trobl.admin.service;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.enums.RoleType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.partner.PartnerRequestRepository;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.PostViewRepository;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.report.Report;
import com.may21.trobl.report.ReportRepository;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.service.TagService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final PartnerRequestRepository partnerRequestRepository;
    private final TagService tagService;
    private final PostViewRepository postViewRepository;
    private final NotificationService notificationService;

    @Transactional
    public boolean grantAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        List<RoleType> adminRoles = new ArrayList<>();
        adminRoles.add(RoleType.ADMIN);
        user.setRoles(adminRoles);

        userRepository.save(user);
        return true;
    }

    @Transactional
    public User revokeRole(Long userId, RoleType roleToRevoke) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        user.removeRole(roleToRevoke); // ⭐️ 특정 권한 제거

        return userRepository.save(user);
    }

    @Transactional
    public User updateAllRoles(Long userId, List<RoleType> newRoles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        user.setRoles(newRoles); // ⭐️ 모든 권한을 새 리스트로 교체

        return userRepository.save(user);
    }

    // ========== 대시보드 관련 메서드 ==========

    @Transactional(readOnly = true)
    public AdminDto.DashboardStats getDashboardStats() {
        // 기본 통계
        long totalUsers = userRepository.count();
        long totalPosts = postRepository.count();
        long totalReports = reportRepository.count();

        // 활성 사용자 수 (최근 30일 내 로그인)
        LocalDate thirtyDaysAgo = LocalDate.from(LocalDateTime.now()
                .minusDays(30));
        long activeUsers = userRepository.countByLastLoginDateAfter(thirtyDaysAgo);

        // 사용자 세분화
        long virtualUsers = userRepository.findByTestUserIsTrue(Pageable.unpaged())
                .getTotalElements();
        long realUsers =
                userRepository.findByTestUserIsFalseAndUnregisteredIsFalse(Pageable.unpaged())
                        .getTotalElements();
        long oAuthUsers = userRepository.findAllOAuth()
                .size();
        long unverifiedUsers = userRepository.countByUnregistered(true);

        // 게시글 상태
        long reportedPosts = postRepository.findAllByReportedTrue()
                .size();
        long pendingPosts = postRepository.findAllUnconfirmedPostsByUserIdIn(
                        userRepository.findAll()
                                .stream()
                                .map(User::getId)
                                .toList(), Pageable.unpaged())
                .getTotalElements();

        // 상호작용
        long totalComments = commentRepository.count();
        long totalLikes = postRepository.findAll()
                .stream()
                .mapToLong(posting -> posting.getPostLikes() != null ? posting.getPostLikes()
                        .size() : 0)
                .sum();
        long views = postViewRepository.count();
        long totalViews = postRepository.findAll()
                .stream()
                .mapToLong(Posting::getViewCount)
                .sum();
        totalViews += views;

        // 파트너
        long totalPartnerRequests = partnerRequestRepository.count();
        long approvedPartners = partnerRequestRepository.findAll()
                .stream()
                .filter(request -> request.getStatus() ==
                        com.may21.trobl._global.enums.RequestStatus.ACCEPTED)
                .count();

        // 주간 변화
        LocalDateTime weekAgo = LocalDateTime.now()
                .minusWeeks(1);
        LocalDate weekAgoDate = LocalDate.from(weekAgo);
        long newUsersThisWeek = userRepository.countBySignUpDateAfter(weekAgoDate);
        long newPostsThisWeek =
                postRepository.countByCreatedAtAfter(weekAgo, PostingType.ANNOUNCEMENT);

        return new AdminDto.DashboardStats(totalUsers, totalPosts, totalReports, activeUsers,
                realUsers, virtualUsers, oAuthUsers, unverifiedUsers, reportedPosts, pendingPosts,
                totalComments, totalLikes, totalViews, totalPartnerRequests, approvedPartners,
                newUsersThisWeek, newPostsThisWeek);
    }

    // ========== 사용자 관리 메서드 ==========

    public Page<AdminDto.VirtualUserInfo> getVirtualUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findByTestUserIsTrue(pageable);

        Map<Long, User> userMap = userPage.getContent()
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<User> users = userPage.getContent();
        List<AdminDto.VirtualUserInfo> virtualUserInfos = users.stream()
                .map(user -> {
                    Long partnerId = user.getPartnerId();
                    User partner = userMap.getOrDefault(partnerId, null);
                    return new AdminDto.VirtualUserInfo(user, partner);
                })
                .collect(Collectors.toList());
        return new PageImpl<>(virtualUserInfos, pageable, userPage.getTotalElements());
    }


    @Transactional
    public boolean deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ExceptionCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(userId);
        return true;
    }

    // ========== 게시글 관리 메서드 ==========

    @Transactional(readOnly = true)
    public Page<AdminDto.PostItem> getVirtualPosts(Pageable pageable) {
        List<User> testUser = userRepository.findUserByTestUserIsTrue();
        List<Long> testUserIds = testUser.stream()
                .map(User::getId)
                .toList();
        Page<Posting> postPage = postRepository.findByUserIdIn(testUserIds, pageable);
        Map<Long, User> userMap = userRepository.findByIdIn(testUserIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(postPage.getContent());

        return postPage.map(post -> {
            User user = userMap.get(post.getUserId());
            List<Tag> tags = tagMap.getOrDefault(post.getId(), new ArrayList<>());
            return new AdminDto.PostItem(post, user, tags);
        });
    }


    @Transactional
    public boolean deletePost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(ExceptionCode.POST_NOT_FOUND);
        }
        postRepository.deleteById(postId);
        return true;
    }

    // ========== 신고 관리 메서드 ==========

    public List<AdminDto.ReportInfo> getReports(Pageable pageable) {
        Page<Report> reportPage = reportRepository.findAll(pageable);

        return reportPage.getContent()
                .stream()
                .map(report -> {
                    Posting post = postRepository.findById(report.getTargetId())
                            .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
                    return convertToReportInfo(report, "pending");
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public AdminDto.ReportInfo updateReportStatus(Long reportId,
            AdminDto.UpdateReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.REPORT_NOT_FOUND));

        // status 업데이트 로직 (Report 엔티티에 status 필드가 있다고 가정)
        // report.setStatus(request.getStatus());
        String status = request.getStatus() != null ? request.getStatus() : "pending";
        Posting posting = postRepository.findById(report.getTargetId())
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (request.getStatus() != null) {
            if ("resolved".equals(request.getStatus())) {
                posting.setReported(false); // 신고 해결 시 게시글 상태 업데이트
                reportRepository.delete(report);
            }
            else if ("pending".equals(request.getStatus())) {
                posting.setReported(true); // 신고 대기 상태로 변경
            }
            // 다른 상태 처리 로직 추가 가능
        }
        return convertToReportInfo(report, status);
    }


    private AdminDto.ReportInfo convertToReportInfo(Report report, String result) {
        return new AdminDto.ReportInfo(report.getId(), report.getTargetType()
                .name(), report.getReason()
                .name(), "reporter", // Report 엔티티에 reporter 정보가 있다면 report.getReporter()
                report.getTargetId(), result, report.getReportedAt());
    }

    public List<AdminDto.ReportedListItem> getReportedItems() {
        List<Posting> reportedPosts = postRepository.findAllByReportedTrue();
        List<Comment> reportedComments = commentRepository.findByReportedIsTrue();
        List<Long> reportedPostIds = reportedPosts.stream()
                .map(Posting::getId)
                .toList();
        List<Long> reportedCommentIds = reportedComments.stream()
                .map(Comment::getId)
                .toList();
        List<Report> reportList =
                reportRepository.findAllByItemIdList(ItemType.POST, reportedPostIds,
                        ItemType.COMMENT, reportedCommentIds);
        Set<Long> userIds = reportList.stream()
                .map(Report::getReportedBy)
                .collect(Collectors.toSet());
        List<User> reportedUsers = userRepository.findAllById(userIds);
        Map<Long, User> userMap = reportedUsers.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        List<AdminDto.ReportedListItem> reportedItems = new ArrayList<>();
        Map<Long, List<Report>> postReportMap = reportList.stream()
                .filter(report -> report.getTargetType() == ItemType.POST)
                .collect(Collectors.groupingBy(Report::getTargetId));
        Map<Long, List<Report>> commentReportMap = reportList.stream()
                .filter(report -> report.getTargetType() == ItemType.COMMENT)
                .collect(Collectors.groupingBy(Report::getTargetId));

        for (Posting post : reportedPosts) {
            List<Report> reports = postReportMap.getOrDefault(post.getId(), new ArrayList<>());
            User user = userMap.get(post.getUserId());
            List<AdminDto.ReportedDetails> reportedDetails = reports.stream()
                    .map(report -> new AdminDto.ReportedDetails(report,
                            userMap.get(report.getReportedBy())))
                    .toList();
            reportedItems.add(new AdminDto.ReportedListItem(post, user, reportedDetails));
        }
        for (Comment comment : reportedComments) {
            List<Report> reports =
                    commentReportMap.getOrDefault(comment.getId(), new ArrayList<>());
            User user = userMap.get(comment.getUserId());
            List<AdminDto.ReportedDetails> reportedDetails = reports.stream()
                    .map(report -> new AdminDto.ReportedDetails(report,
                            userMap.get(report.getReportedBy())))
                    .toList();
            reportedItems.add(new AdminDto.ReportedListItem(comment, user, reportedDetails));
        }
        return reportedItems;
    }

    public boolean processReportedItems(Long itemId, ItemType itemType, boolean delete) {
        if (itemType == ItemType.POST) {
            Posting post = postRepository.findById(itemId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
            if (delete) {
                PostDto.Notification info = new PostDto.Notification(post);
                postRepository.delete(post);
                notificationService.notifyPostDeleted(post.getId(), info);
            }
            else {
                post.setReported(false);
                postRepository.save(post);
            }
        }
        else if (itemType == ItemType.COMMENT) {
            Comment comment = commentRepository.findById(itemId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.COMMENT_NOT_FOUND));
            if (delete) {
                commentRepository.delete(comment);

                notificationService.notifyCommentDeleted(comment);
            }
            else {
                comment.setReported(false);
                commentRepository.save(comment);
            }
        }
        else {
            throw new BusinessException(ExceptionCode.INVALID_REQUEST);
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Page<AdminDto.CommentItems> getVirtualComments(Pageable pageable) {
        List<User> testUser = userRepository.findUserByTestUserIsTrue();
        List<Long> testUserIds = testUser.stream()
                .map(User::getId)
                .toList();
        Map<Long, User> userMap = userRepository.findByIdIn(testUserIds)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Page<Comment> comments = commentRepository.findAllByUserIdsIn(testUserIds, pageable);
        Map<Long, Posting> postMap = comments.stream()
                .collect(Collectors.toMap(Comment::getId, Comment::getPosting));
        return comments.map(comment -> {
            Posting post = postMap.get(comment.getId());
            User user = userMap.get(comment.getUserId());
            return new AdminDto.CommentItems(post, comment, user);
        });
    }
}
