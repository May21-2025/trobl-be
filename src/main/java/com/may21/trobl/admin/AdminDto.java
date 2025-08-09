package com.may21.trobl.admin;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.enums.ReportType;
import com.may21.trobl.admin.announcement.Announcement;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.report.Report;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.may21.trobl._global.utility.SecurityUtils.decodeHtml;

public class AdminDto {

    // ========== 대시보드 관련 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class DashboardStats {
        // 기본 통계
        private final Long totalUsers;
        private final Long totalPosts;
        private final Long totalReports;
        private final Long activeUsers;

        // 사용자 세분화
        private final Long realUsers;        // 실제 사용자
        private final Long virtualUsers;     // 가상유저
        private final Long oAuthUsers;       // 소셜 로그인 사용자
        private final Long unverifiedUsers;  // 미인증 사용자

        // 게시글 상태
        private final Long reportedPosts;    // 신고된 게시글
        private final Long pendingPosts;     // 승인 대기 게시글

        // 상호작용
        private final Long totalComments;    // 총 댓글
        private final Long totalLikes;       // 총 좋아요
        private final Long totalViews;       // 총 조회수

        // 파트너
        private final Long totalPartnerRequests;  // 총 파트너 요청
        private final Long approvedPartners;      // 승인된 파트너

        // 주간 변화
        private final Long newUsersThisWeek;
        private final Long newPostsThisWeek;
    }

    // ========== 사용자 관리 관련 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class UserListResponse {
        private final List<UserDto.Info> content;
        private final Long totalElements;
        private final Integer totalPages;
        private final Integer currentPage;
        private final Integer size;
    }


    @Getter
    public static class VirtualUserInfo extends UserDto.Info {
        private final UserDto.Info partnerInfo;
        private final LocalDate marriageDate;

        public VirtualUserInfo(User user, User partner) {
            super(user);
            this.partnerInfo = partner != null ? new UserDto.Info(partner) : null;
            this.marriageDate = user.getWeddingAnniversaryDate();
        }
    }

    // ========== 게시글 관리 관련 DTO ==========


    @Getter
    @AllArgsConstructor
    public static class PostInfo {
        private Long postId;
        private UserDto.Info user;
        private String title;
        private String content;
        private boolean confirmed;
        private LocalDateTime createdAt;
        private final PostDto.PollDto poll;
        private final List<PostDto.FairViewItem> fairViewItems;
        public PostInfo(RedisDto.PostDto postDto,List<RedisDto.FairViewDto> fairViews,
                RedisDto.PollDto pollDto, List<RedisDto.PollOptionDto> optionDtoList, RedisDto.UserDto userDto) {
            this.postId = postDto.getPostId();
            this.user = new UserDto.Info(userDto);
            this.title = postDto.getTitle();
            this.content = postDto.getContent();
            String createdAt = postDto.getCreatedAt();
            this.createdAt = LocalDateTime.parse(createdAt);
            this.confirmed = postDto.isConfirmed();
            // PollDto 생성
            if (pollDto != null && optionDtoList != null) {
                List<PostDto.PollItem> pollItems = new ArrayList<>();
                for (RedisDto.PollOptionDto optionDto : optionDtoList) {
                    boolean voted = false;
                    pollItems.add(new PostDto.PollItem(optionDto.getPollOptionId(), optionDto.getName(),
                            optionDto.getVoteCount(), optionDto.getIndex(), voted));
                }
                this.poll = new PostDto.PollDto(pollDto.getPollId(), pollDto.getTitle(),
                        pollDto.isAllowMultipleVotes(), true, pollItems);
            }
            else {
                this.poll = null;
            }

            // FairViewItems 생성
            this.fairViewItems = fairViews == null ? null : fairViews.stream()
                    .map(fairView -> new PostDto.FairViewItem(fairView.getFairViewId(),
                            fairView.getUserId(), fairView.getTitle(), fairView.getNickname(),
                            fairView.getContent()))
                    .toList();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class UpdatePostRequest {
        private String title;
        private String content;
        private String status; // published, draft, deleted
    }

    // ========== 신고 관리 관련 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class ReportInfo {
        private Long id;
        private String type; // user, post, comment
        private String reason;
        private String reporter;
        private Long targetId;
        private String status; // pending, reviewed, resolved
        private LocalDateTime createdAt;
    }

    @Getter
    @AllArgsConstructor
    public static class UpdateReportRequest {
        private String status; // pending, reviewed, resolved
    }

    // ========== 알림 관리 관련 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class NotificationInfo {
        private Long id;
        private String title;
        private String message;
        private String type; // info, warning, error, success
        private LocalDateTime createdAt;
        private Boolean read;
    }

    // ========== 설정 관리 관련 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class SettingsInfo {
        private String siteName;
        private Boolean maintenanceMode;
        private Long maxFileSize;
        private List<String> allowedFileTypes;
        private NotificationSettings notificationSettings;
    }

    @Getter
    @AllArgsConstructor
    public static class NotificationSettings {
        private Boolean emailNotifications;
        private Boolean pushNotifications;
    }

    @Getter
    @AllArgsConstructor
    public static class UpdateSettingsRequest {
        private String siteName;
        private Boolean maintenanceMode;
        private Long maxFileSize;
        private List<String> allowedFileTypes;
        private NotificationSettings notificationSettings;
    }

    // ========== 기존 푸시 알림 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class PushNotification {
        private Long userId;
        private String title;
        private String message;
        private Map<String, String> data;
    }

    // ========== 페이지네이션 요청 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class PageRequest {
        private Integer page = 0;
        private Integer size = 20;
        private String sortBy = "createdAt";
        private String sortDirection = "desc";
    }

    // ========== 검색 요청 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class SearchRequest {
        private String keyword;
        private String type; // user, post, report
        private String status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer page = 0;
        private Integer size = 20;
    }

    // ========== 통계 요청 DTO ==========

    @Getter
    @AllArgsConstructor
    public static class StatsRequest {
        private String period;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    @Getter
    @AllArgsConstructor
    public static class ConnectPartners {
        private Long userId;
        private Long partnerId;
        private LocalDate marriageDate;
    }

    @Getter
    public static class UserDetails extends UserDto.Info {
        private final boolean partnerConnected;
        private final boolean married;
        private final String role;

        public UserDetails(User user) {
            super(user);

            this.partnerConnected = user.getPartnerId() != null;
            this.married = user.isMarried();
            this.role = user.getRole();
        }
    }

    @Getter
    public static class VirtualPostRequest extends PostDto.Request {
        private final Long userId;

        public VirtualPostRequest(String title, String content, Long userId, String postType,
                PostDto.FairViewItem fairViewItem, PostDto.PollRequest poll,
                List<TagDto.Request> tags) {
            super(title, content, postType, fairViewItem, poll, tags);
            this.userId = userId;
        }

    }

    @Getter
    public static class PostItem extends PostDto.BasePostDto {
        private final String nickname;
        private final PostingType postType;
        private final LocalDateTime createdAt;

        public PostItem(Posting posting, User user, List<Tag> tags) {
            super(posting, user, tags);
            this.nickname = user.getNickname();
            this.postType = posting.getPostType();
            this.createdAt = posting.getCreatedAt();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class FairViewPostRequest {
        private final Long userId;
        private final String title;
        private final String content;
        private final String postType;
        private final List<FairViewRequest> fairViewItem;
        private final List<TagDto.Request> tags;

        public PostingType getPostType() {
            return PostingType.fromString(postType);
        }
    }


    @Getter
    @AllArgsConstructor
    public static class FairViewRequest {
        private final Long userId;
        private final String title;
        private final String content;

    }

    @Getter
    public static class ReportedListItem {
        private final Long userId;
        private final String nickname;
        private final ItemType itemType;
        private final Long itemId;
        private final String title;
        private final String content;
        private final List<ReportedDetails> reportedDetails;

        public ReportedListItem(Posting posting, User user, List<ReportedDetails> reportedDetails) {
            this.userId = user.getId();
            this.nickname = user.getNickname();
            this.itemType = ItemType.POST;
            this.itemId = posting.getId();
            this.title = posting.getTitle();
            this.content = posting.getContent();
            this.reportedDetails = reportedDetails;
        }

        public ReportedListItem(User user, List<ReportedDetails> reportedDetails) {
            this.userId = user.getId();
            this.nickname = user.getNickname();
            this.itemType = ItemType.USER;
            this.itemId = user.getId();
            this.title = "user";
            this.content = user.getNickname();
            this.reportedDetails = reportedDetails;
        }

        public ReportedListItem(Comment comment, User user, List<ReportedDetails> reportedDetails) {
            this.userId = user.getId();
            this.nickname = user.getNickname();
            this.itemType = ItemType.COMMENT;
            this.itemId = comment.getId();
            this.title = "comment";
            this.content = comment.getContent();
            this.reportedDetails = reportedDetails;
        }

    }

    @Getter
    public static class ReportedDetails {

        private final String nickname;
        private final Long userId;
        private final ReportType reportType;
        private final LocalDateTime reportedAt;

        public ReportedDetails(Report repost, User user) {
            this.nickname = user.getNickname();
            this.userId = user.getId();
            this.reportType = repost.getReason();
            this.reportedAt = repost.getReportedAt();
        }
    }


    @Getter
    public static class AnnouncementDto {
        private final Long announcementId;
        private final String title;
        private final String content;
        private final boolean liked;
        private final long likeCount;
        private final long viewCount;
        private final LocalDateTime createdAt;

        public AnnouncementDto(Announcement announcement, boolean liked, int likeCount) {
            this.announcementId = announcement.getId();
            this.title = decodeHtml(announcement.getTitle());
            this.content = decodeHtml(announcement.getContent());
            this.createdAt = announcement.getCreateAt();
            this.viewCount = announcement.getViewCount();
            this.liked = liked;
            this.likeCount = likeCount;
        }
    }

    @Getter
    public static class VirtualCommentRequest {
        private String content;
        private Long commentId;
        private Long postId;
        private Long userId;
    }
    @Getter
    public static class CommentInfo {
        private final Long commentId;
        private final UserDto.Info user;
        private final String content;
        private final LocalDateTime createdAt;
        private final Long parentCommentId;

        public CommentInfo(Comment comment, User user) {
            this.commentId = comment.getId();
            this.content = decodeHtml(comment.getContent());
            this.createdAt = comment.getCreatedAt();
            this.user = new UserDto.Info(user);
            this.parentCommentId = getParentCommentId();

        }

    }
    @Getter
    public static class CommentItems {
        private final PostParent postInfo;
        private final Long commentId;
        private final UserDto.Info user;
        private final String content;
        private final LocalDateTime createdAt;
        private final boolean hasParentComment;

        public CommentItems(Posting post, Comment comment, User user) {
            this.postInfo = new PostParent(post);
            this.commentId = comment.getId();
            this.content = decodeHtml(comment.getContent());
            this.createdAt = comment.getCreatedAt();
            this.user = new UserDto.Info(user);
            this.hasParentComment = comment.getParentComment() != null;

        }

    }

    @Getter
    public static class PostParent {
        private final Long postId;
        private final String title;

        public PostParent(Posting post) {

            this.postId = post.getId();
            this.title = decodeHtml(post.getTitle());
        }
    }

}
