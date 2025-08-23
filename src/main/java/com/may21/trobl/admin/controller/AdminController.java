package com.may21.trobl.admin.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.NotificationStrategy;
import com.may21.trobl._global.enums.NotificationType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl._global.utility.Utility;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.admin.service.AdminService;
import com.may21.trobl.auth.AuthDto;
import com.may21.trobl.auth.jwt.TokenInfo;
import com.may21.trobl.comment.dto.CommentDto;
import com.may21.trobl.comment.service.CommentService;
import com.may21.trobl.notification.domain.ContentUpdateService;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.redis.CacheService;
import com.may21.trobl.storage.StorageService;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    private final PostingService postingService;
    private final NotificationService notificationService;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;
    private final AdminService adminService;
    private final StorageService storageService;

    private static final Long TEST_USER_ID = 42L;
    private final ContentUpdateService contentUpdateService;
    private final CacheService cacheService;
    private final CommentService commentService;

    // ========== 대시보드 API ==========



    @GetMapping("/authenticate")
    public ResponseEntity<Message> authenticateAdmin(@RequestHeader("Authorization") String token) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token)
                .getId();
        boolean response = adminService.grantAdminRole(userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/users")
    public ResponseEntity<Message> getAllUsers(@RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        jwtTokenUtil.getAdminUserByToken(token);
        Page<AdminDto.UserDetails> response =
                userService.getAllUsers(page, size, sortBy, sortDirection);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Message> deleteUser(@RequestHeader("Authorization") String token,
            @PathVariable Long userId) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = adminService.deleteUser(userId);
        contentUpdateService.deleteItem(userId, ItemType.USER);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/data-organize")
    public ResponseEntity<Message> dataOrganize(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        adminService.dataOrganize();
        cacheService.invalidateAllPostRelatedCache();
        postingService.evictAllTopPosts();
        return new ResponseEntity<>(Message.success(true), HttpStatus.OK);
    }

    // ========== 게시글 관리 API ==========
    @PostMapping("/announcements/account")
    public ResponseEntity<Message> getAdminAccount(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        UserDto.Info response = userService.getAdminAccount();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/announcements/account")
    public ResponseEntity<Message> updateAdminAccount(@RequestHeader("Authorization") String token,
            @RequestPart UserDto.Update request,
            @RequestPart(required = false) MultipartFile thumbnail) {
        jwtTokenUtil.getAdminUserByToken(token);
        UserDto.Info response = userService.updateAdminAccount(request);
        if (thumbnail != null) {
            String imageKey =
                    storageService.uploadUserProfileImage(response.getUserId(), thumbnail);
            userService.setThumbnail(response.getUserId(), imageKey);
            response.setThumbnailUrl(Utility.getUserProfileUrl(imageKey));
        }
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/announcements")
    public ResponseEntity<Message> createAnnouncement(@RequestHeader("Authorization") String token,
            @RequestBody AdminDto.VirtualPostRequest createRequest) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdminDto.PostInfo response = postingService.createAnnouncement(createRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/announcements/{postId}")
    public ResponseEntity<Message> updateAnnouncement(@RequestHeader("Authorization") String token,
            @PathVariable Long postId, @RequestBody AdminDto.VirtualPostRequest updateRequest) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdminDto.PostInfo response = postingService.updateAnnouncement(postId, updateRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/tags/search")
    public ResponseEntity<Message> getTagSearch(@RequestHeader("Authorization") String token,
            @RequestParam String keyword) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<String> response = adminService.getSearchedTags(keyword);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);

    }


    @GetMapping("/posts")
    public ResponseEntity<Message> getAllPosts(@RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "createdAt") String sortType,
            @RequestParam(defaultValue = "postTypes") List<String> postTypes,
            @RequestParam(defaultValue = "true") boolean asc) {
        jwtTokenUtil.getAdminUserByToken(token);
        Page<PostDto.AdminListItem> response =
                postingService.getAdminAllPosts(size, page, sortType, asc, postTypes);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/posts/List")
    public ResponseEntity<Message> getAllDetailedPosts(@RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "createdAt") String sortType,
            @RequestParam(defaultValue = "", required = false) List<String> postTypes,
            @RequestParam(defaultValue = "", required = false) List<Long> tagIds,
            @RequestParam(defaultValue = "false") boolean asc) {
        jwtTokenUtil.getAdminUserByToken(token);
        Page<AdminDto.PostListItem> response =
                adminService.getAllDetailedPosts(size, page, sortType, asc, postTypes, tagIds);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/posts/admin-tags")
    public ResponseEntity<Message> setAdminTagsToPosts(
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        adminService.makeTagsForPosts();
        return new ResponseEntity<>(Message.success(true), HttpStatus.OK);
    }

    @GetMapping("/posts/details/refresh")
    public ResponseEntity<Message> updatePostDetailInfos(
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        adminService.updatePostDetailInfos();
        return new ResponseEntity<>(Message.success(true), HttpStatus.OK);
    }


    @GetMapping("/posts/{postId}")
    public ResponseEntity<Message> getPosts(@RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdminDto.PostInfo response = postingService.getAdminPostInfo(postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Message> deletePost(@RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = adminService.deletePost(postId);
        contentUpdateService.deleteItem(postId, ItemType.POST);
        cacheService.invalidatePostRelatedCache(postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<Message> getPostComments(@RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<AdminDto.CommentInfo> response = commentService.getAdminCommentInfo(postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/reported-items")
    public ResponseEntity<Message> getReportedPosts(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<AdminDto.ReportedListItem> response = adminService.getReportedItems();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PatchMapping("/reported-items/{itemId}")
    public ResponseEntity<Message> unblockPost(@RequestHeader("Authorization") String token,
            @PathVariable Long itemId, @RequestParam ItemType itemType,
            @RequestParam boolean delete) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = adminService.processReportedItems(itemId, itemType, delete);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/user-token/{userId}")
    public ResponseEntity<Message> getUserToken(@RequestHeader("Authorization") String token,
            @PathVariable Long userId, HttpServletResponse response) {
        jwtTokenUtil.getAdminUserByToken(token);
        User user = userService.getUser(userId);
        TokenInfo userToken =
                jwtTokenUtil.generateAccessAndRefreshToken(user, null, null, "unknown");
        userToken.tokenToHeaders(response);
        return new ResponseEntity<>(Message.success(true), HttpStatus.OK);
    }


    // ========== 콘텐츠 시뮬레이터 기능 ==========

    @GetMapping("/content-simulator/users")
    public ResponseEntity<Message> getVirtualUsers(@RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        jwtTokenUtil.getAdminUserByToken(token);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminDto.VirtualUserInfo> response = adminService.getVirtualUsers(pageable);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }


    @PostMapping("/content-simulator/users")
    public ResponseEntity<Message> createVirtualUsers(@RequestHeader("Authorization") String token,
            @RequestPart AuthDto.SignUpRequest signUpRequest,
            @RequestPart(required = false) MultipartFile thumbnail) {
        jwtTokenUtil.getAdminUserByToken(token);
        UserDto.Info response = userService.createVirtualUsers(signUpRequest);
        if (thumbnail != null) {
            String imageKey =
                    storageService.uploadUserProfileImage(response.getUserId(), thumbnail);
            userService.setThumbnail(response.getUserId(), imageKey);
            response.setThumbnailUrl(Utility.getUserProfileUrl(imageKey));
        }
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/content-simulator/users/search")
    public ResponseEntity<Message> createVirtualUsers(@RequestHeader("Authorization") String token,
            @RequestParam String keyword) {
        jwtTokenUtil.getAdminUserByToken(token);
        List<AdminDto.SearchedUser> response = adminService.createVirtualUsers(keyword);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);

    }

    @GetMapping("/content-simulator/users/{userId}/check")
    public ResponseEntity<Message> isVirtualUsers(@PathVariable Long userId,
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = userService.isVirtualUsers(userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/content-simulator/users/{userId}")
    public ResponseEntity<Message> updateVirtualUsers(@PathVariable Long userId,
            @RequestHeader("Authorization") String token, @RequestPart UserDto.Update request,
            @RequestPart(required = false) MultipartFile thumbnail) {
        jwtTokenUtil.getAdminUserByToken(token);
        UserDto.Info response = userService.updateVirtualUsers(userId, request);
        if (thumbnail != null) {
            String imageKey =
                    storageService.uploadUserProfileImage(response.getUserId(), thumbnail);
            userService.setThumbnail(response.getUserId(), imageKey);
            response.setThumbnailUrl(Utility.getUserProfileUrl(imageKey));
        }
        cacheService.invalidateUserCache(userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/content-simulator/users/partner")
    public ResponseEntity<Message> connectPartners(@RequestHeader("Authorization") String token,
            @RequestBody AdminDto.ConnectPartners request) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = userService.connectPartners(request);
        cacheService.invalidateUserCache(request.getUserId());
        cacheService.invalidateUserCache(request.getPartnerId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/content-simulator/posts")
    public ResponseEntity<Message> getPosts(@RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        jwtTokenUtil.getAdminUserByToken(token);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        Page<AdminDto.PostItem> response = adminService.getVirtualPosts(pageable);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/content-simulator/posts")
    public ResponseEntity<Message> createVirtualPosts(@RequestHeader("Authorization") String token,
            @RequestBody AdminDto.VirtualPostRequest createRequest) {
        jwtTokenUtil.getAdminUserByToken(token);
        PostDto.ListItem response = postingService.createVirtualPost(createRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/content-simulator/posts/fair-view")
    public ResponseEntity<Message> createVirtualPosts(@RequestHeader("Authorization") String token,
            @RequestBody AdminDto.FairViewPostRequest request) {
        jwtTokenUtil.getAdminUserByToken(token);
        PostDto.ListItem response = postingService.createFairViewByAdmin(request);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/content-simulator/posts/{postId}")
    public ResponseEntity<Message> updateVirtualPost(@RequestHeader("Authorization") String token,
            @PathVariable Long postId, @RequestBody PostDto.Request updateRequest) {
        jwtTokenUtil.getAdminUserByToken(token);
        PostDto.ListItem response = postingService.updateVirtualPost(postId, updateRequest);
        cacheService.invalidatePostRelatedCache(postId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/content-simulator/fair-view/{fairViewId}")
    public ResponseEntity<Message> updateVirtualFairView(
            @RequestHeader("Authorization") String token, @PathVariable Long fairViewId,
            @RequestBody PostDto.FairViewRequest request) {
        jwtTokenUtil.getAdminUserByToken(token);
        PostDto.FairViewItem response = postingService.updateVirtualFairView(fairViewId, request);
        Long postId = postingService.getPostIdByFairViewId(fairViewId);
        cacheService.evictFairViewFromCache(postId, fairViewId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/content-simulator/poll/{pollId}")
    public ResponseEntity<Message> updateVirtualPoll(@RequestHeader("Authorization") String token,
            @PathVariable Long pollId, @RequestBody PostDto.PollRequest request) {
        jwtTokenUtil.getAdminUserByToken(token);
        PostDto.PollDto response = postingService.updatePoll(pollId, request);
        Long postId = postingService.getPostIdByPollId(pollId);
        cacheService.invalidatePollCache(postId, pollId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/content-simulator/comments")
    public ResponseEntity<Message> getComments(@RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        jwtTokenUtil.getAdminUserByToken(token);
        Pageable pageable = Utility.getPageable(page, size, sortBy, sortDirection);
        Page<AdminDto.CommentItems> response = adminService.getVirtualComments(pageable);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/content-simulator/comments")
    public ResponseEntity<Message> createVirtualComments(
            @RequestHeader("Authorization") String token,
            @RequestBody AdminDto.VirtualCommentRequest createRequest) {
        jwtTokenUtil.getAdminUserByToken(token);
        CommentDto.Response response = commentService.createVirtualComment(createRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/content-simulator/comments/{commentId}")
    public ResponseEntity<Message> updateVirtualComments(
            @RequestHeader("Authorization") String token, @PathVariable Long commentId,
            @RequestBody AdminDto.VirtualCommentRequest createRequest) {
        jwtTokenUtil.getAdminUserByToken(token);
        CommentDto.Response response =
                commentService.updateVirtualComments(commentId, createRequest);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @DeleteMapping("/content-simulator/comments/{commentId}")
    public ResponseEntity<Message> deleteVirtualComments(
            @RequestHeader("Authorization") String token, @PathVariable Long commentId) {
        jwtTokenUtil.getAdminUserByToken(token);
        boolean response = commentService.deleteVirtualComments(commentId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    // ========== 42번 유저 알림 테스트 기능 ==========

    /**
     * 42번 유저에게 모든 알림 유형을 테스트로 전송 (3초 간격)
     */
    @PostMapping("/notifications/test-all")
    public ResponseEntity<Message> testAllNotificationTypes(
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        log.debug("Starting notification test for user ID: {} with 3 second intervals",
                TEST_USER_ID);

        int sentCount = 0;
        NotificationType[] types = NotificationType.values();

        for (int i = 0; i < types.length; i++) {
            NotificationType type = types[i];
            try {
                sendTestNotificationByType(type);
                sentCount++;
                log.debug("Sent test notification: {} ({}/{})", type.name(), i + 1, types.length);

                // 마지막 알림이 아니면 3초 대기
                if (i < types.length - 1) {
                    Thread.sleep(3000); // 3초 대기
                    log.debug("Waiting 3 seconds before next notification...");
                }
            } catch (InterruptedException e) {
                log.warn("Notification test interrupted at type: {}", type.name());
                Thread.currentThread()
                        .interrupt(); // 인터럽트 상태 복원
                break;
            } catch (Exception e) {
                log.error("Failed to send notification type: {}", type.name(), e);
            }
        }

        String resultMessage =
                String.format("테스트 완료! %d/%d개의 알림을 42번 유저에게 전송했습니다. (총 소요시간: 약 %d초)", sentCount,
                        types.length, (sentCount - 1) * 3);

        return new ResponseEntity<>(Message.success(resultMessage), HttpStatus.OK);
    }

    /**
     * 특정 알림 유형만 42번 유저에게 테스트
     */
    @PostMapping("/notifications/test-type/{notificationType}")
    public ResponseEntity<Message> testSpecificNotificationType(
            @RequestHeader("Authorization") String token, @PathVariable String notificationType) {

        jwtTokenUtil.getAdminUserByToken(token);

        NotificationType type = NotificationType.valueOf(notificationType.toUpperCase());
        sendTestNotificationByType(type);

        String resultMessage = String.format("%s 알림을 42번 유저에게 전송했습니다.", type.name());
        return new ResponseEntity<>(Message.success(resultMessage), HttpStatus.OK);
    }

    /**
     * 즉시 전송 테스트
     */
    @PostMapping("/notifications/test-immediate")
    public ResponseEntity<Message> testImmediateNotification(
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);


        notificationService.testNotification(TEST_USER_ID, NotificationType.ANNOUNCEMENT,
                "🚀 즉시 전송 테스트", "즉시 전송 알림이 잘 작동하는지 테스트합니다!",
                Map.of("itemType", "post", "itemId", "1"), NotificationStrategy.IMMEDIATE);

        return new ResponseEntity<>(Message.success("즉시 전송 테스트 완료"), HttpStatus.OK);
    }

    /**
     * 일괄 전송 테스트
     */
    @PostMapping("/notifications/test-batched")
    public ResponseEntity<Message> testBatchedNotification(
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);

        // 여러 개의 일괄 처리 알림을 연속으로 보내서 테스트
        for (int i = 1; i <= 3; i++) {
            notificationService.testNotification(TEST_USER_ID, NotificationType.LIKE,
                    "👍 좋아요 알림 " + i, "일괄 처리 테스트용 좋아요 알림입니다.",
                    Map.of("itemType", "post", "itemId", "1"), NotificationStrategy.BATCHED);
        }

        return new ResponseEntity<>(Message.success("일괄 전송 테스트 완료 (3개 알림 큐잉됨)"), HttpStatus.OK);
    }

    /**
     * 예약 전송 테스트 (1분 후)
     */
    @PostMapping("/notifications/test-scheduled")
    public ResponseEntity<Message> testScheduledNotification(
            @RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);

        LocalDateTime scheduledTime = LocalDateTime.now()
                .plusMinutes(1);
        String formattedTime = scheduledTime.toString();
        notificationService.testNotification(TEST_USER_ID, NotificationType.MARKETING,
                "⏰ 예약 전송 테스트 ", formattedTime + "에 받게 될 예약 알림입니다!",
                Map.of("itemType", "post", "itemId", "1", "scheduledTime",
                        scheduledTime.toString()), NotificationStrategy.SCHEDULED);
        return new ResponseEntity<>(Message.success("예약 전송 테스트 완료 (1분 후 전송 예정)"), HttpStatus.OK);
    }

    @GetMapping("/organize")
    public ResponseEntity<Message> deletedeletedNoti(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        return new ResponseEntity<>(Message.success(""), HttpStatus.OK);
    }

    /**
     * 알림 유형별 테스트 메시지 생성
     */
    private void sendTestNotificationByType(NotificationType type) {
        try {

            String title = getTestTitle(type);
            String body = getTestBody(type);
            Map<String, String> data = getTestData(type);
            NotificationStrategy strategy = type.getDefaultStrategy();

            // 예약 전송의 경우 30초 후로 설정
            if (strategy == NotificationStrategy.SCHEDULED) {
                LocalDateTime scheduledTime = LocalDateTime.now()
                        .plusSeconds(30);
                data.put("scheduledTime", scheduledTime.toString());
                notificationService.testNotification(TEST_USER_ID, type, title, body, data,
                        strategy);
            }
            else {
                notificationService.testNotification(TEST_USER_ID, type, title, body, data,
                        strategy);
            }
        } catch (Exception e) {
            log.error("Failed to send test notification for type: {}", type.name(), e);
            throw new BusinessException(ExceptionCode.TEST_FAILED);
        }
    }

    private String getTestTitle(NotificationType type) {
        return switch (type) {
            case COMMENT -> "💬 새 댓글 테스트";
            case LIKE -> "👍 좋아요 테스트";
            case VOTE -> "🗳️ 투표 테스트";
            case CONTENT_RECOMMENDATION -> "📚 콘텐츠 추천 테스트";
            case POPULAR_POST -> "🔥 인기 게시글 테스트";
            case FAIRVIEW_REQUEST -> "⚖️ 페어뷰 요청 테스트";
            case FAIRVIEW_CONFIRMATION -> "✅ 페어뷰 확인 테스트";
            case QUICK_POLL_PARTICIPATION -> "⚡ 빠른 투표 참여 테스트";
            case COMMUNITY_ANALYSIS -> "📊 커뮤니티 분석 테스트";
            case ANNOUNCEMENT -> "📢 공지사항 테스트";
            case POST_DELETED -> "🗑️ 게시글 삭제 테스트";
            case COMMENT_DELETED -> "🗑️ 댓글 삭제 테스트";
            case MARKETING -> "🎁 마케팅 알림 테스트";
            case PARTNER_ACCEPTED -> "🤝 파트너 요청 승인 테스트";
            case PARTNER_DECLINED -> "❌ 파트너 요청 거절 테스트";
            case PARTNER_REQUEST -> "🤝 파트너 요청 테스트";
            case ETC -> "📝 기타 알림 테스트";
        };
    }

    private String getTestBody(NotificationType type) {
        return switch (type) {
            case COMMENT -> "테스트용 댓글이 달렸습니다!";
            case LIKE -> "테스트용 좋아요를 받았습니다!";
            case VOTE -> "테스트 투표에 참여해주세요!";
            case CONTENT_RECOMMENDATION -> "회원님께 추천하는 테스트 콘텐츠가 있어요!";
            case POPULAR_POST -> "지금 인기있는 테스트 게시글을 확인해보세요!";
            case FAIRVIEW_REQUEST -> "페어뷰 요청이 도착했습니다. (테스트)";
            case FAIRVIEW_CONFIRMATION -> "페어뷰가 확인되었습니다. (테스트)";
            case QUICK_POLL_PARTICIPATION -> "빠른 테스트 투표에 참여해주세요!";
            case COMMUNITY_ANALYSIS -> "이번 주 커뮤니티 분석 결과입니다. (테스트)";
            case ANNOUNCEMENT -> "중요한 테스트 공지사항이 있습니다!";
            case POST_DELETED -> "테스트 게시글이 삭제되었습니다.";
            case COMMENT_DELETED -> "테스트 댓글이 삭제되었습니다.";
            case MARKETING -> "특별한 테스트 이벤트가 진행중입니다!";
            case PARTNER_ACCEPTED -> "테스트 파트너 요청이 승인되었습니다.";
            case PARTNER_DECLINED -> "테스트 파트너 요청이 거절되었습니다.";
            case PARTNER_REQUEST -> "테스트 파트너 요청이 도착했습니다.";
            case ETC -> "기타 테스트 알림입니다.";
        };
    }

    private Map<String, String> getTestData(NotificationType type) {
        Map<String, String> data = new HashMap<>();
        data.put("type", type.name()
                .toLowerCase());
        data.put("isTest", "true");
        data.put("testTimestamp", String.valueOf(System.currentTimeMillis()));

        // 타입별 추가 데이터
        switch (type) {
            case COMMENT, LIKE, VOTE -> {
                data.put("postId", "999999");
                data.put("commentId", "888888");
            }
            case FAIRVIEW_REQUEST, FAIRVIEW_CONFIRMATION -> {
                data.put("fairViewId", "777777");
                data.put("postId", "999999");
            }
            case CONTENT_RECOMMENDATION, POPULAR_POST -> {
                data.put("recommendedPostId", "666666");
            }
            case MARKETING -> {
                data.put("eventId", "test_event_2025");
                data.put("couponCode", "TEST20");
            }
        }

        return data;
    }


}
