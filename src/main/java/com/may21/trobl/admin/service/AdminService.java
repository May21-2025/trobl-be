package com.may21.trobl.admin.service;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.enums.RoleType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.admin.domain.PostDetailInfo;
import com.may21.trobl.admin.repository.PostDetailInfoRepository;
import com.may21.trobl.comment.domain.Comment;
import com.may21.trobl.comment.domain.CommentRepository;
import com.may21.trobl.comment.service.CommentService;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.poll.domain.PollVote;
import com.may21.trobl.poll.repository.VoteRepository;
import com.may21.trobl.post.domain.*;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.recordLimit.repository.RecordTrackRepository;
import com.may21.trobl.redis.CacheService;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.report.Report;
import com.may21.trobl.report.ReportRepository;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.repository.TagMappingRepository;
import com.may21.trobl.tag.repository.TagRepository;
import com.may21.trobl.tag.service.TagService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.may21.trobl._global.utility.PostExamineTagValue.KEYWORD_MAPPINGS;
import static com.may21.trobl._global.utility.PostExamineTagValue.TAG_POOL;
import static com.may21.trobl._global.utility.Utility.findTagsContainingKeyword;
import static com.may21.trobl._global.utility.Utility.getFilteredPostTypeList;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final TagService tagService;
    private final PostViewRepository postViewRepository;
    private final PostLikeRepository postLikeRepository;
    private final VoteRepository voteRepository;
    private final NotificationService notificationService;
    private final FairViewRepository fairViewRepository;

    private final CacheService cacheService;
    private final TagMappingRepository tagMappingRepository;
    private final PostDetailInfoRepository postDetailInfoRepository;
    private final CommentService commentService;
    private final TagRepository tagRepository;
    private final RecordTrackRepository recordTrackRepository;


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
                .map(user -> user.getId())
                .toList();
        Page<Posting> postPage = postRepository.findByUserIdIn(testUserIds, pageable);
        Map<Long, User> userMap = userRepository.findByIdIn(testUserIds)
                .stream()
                .collect(Collectors.toMap(user -> user.getId(), user -> user));
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
                .map(report -> report.getReportedBy())
                .collect(Collectors.toSet());
        List<User> reportedUsers = userRepository.findAllById(userIds);
        Map<Long, User> userMap = reportedUsers.stream()
                .collect(Collectors.toMap(user -> user.getId(), user -> user));
        List<AdminDto.ReportedListItem> reportedItems = new ArrayList<>();
        Map<Long, List<Report>> postReportMap = reportList.stream()
                .filter(report -> report.getTargetType() == ItemType.POST)
                .collect(Collectors.groupingBy(report -> report.getTargetId()));
        Map<Long, List<Report>> commentReportMap = reportList.stream()
                .filter(report -> report.getTargetType() == ItemType.COMMENT)
                .collect(Collectors.groupingBy(report -> report.getTargetId()));

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
                .map(user -> user.getId())
                .toList();
        Map<Long, User> userMap = userRepository.findByIdIn(testUserIds)
                .stream()
                .collect(Collectors.toMap(user -> user.getId(), user -> user));
        Page<Comment> comments = commentRepository.findAllByUserIdsIn(testUserIds, pageable);
        Map<Long, Posting> postMap = comments.stream()
                .collect(Collectors.toMap(comment -> comment.getId(),
                        comment -> comment.getPosting()));
        return comments.map(comment -> {
            Posting post = postMap.get(comment.getId());
            User user = userMap.get(comment.getUserId());
            return new AdminDto.CommentItems(post, comment, user);
        });
    }

    @Transactional
    public void dataOrganize() {
        log.info("deletePosts");
        List<Posting> allPost = postRepository.findAllIdExceptAnnouncement(652L);
        postRepository.deleteAll(allPost);
        log.info("done");
    }

    @Transactional
    public void makeTagsForPosts() {
        List<Posting> allPost = postRepository.findAllHasNoAdminTagged(PostingType.ANNOUNCEMENT);

        // 기존 admin 태그만 삭제
        tagMappingRepository.deleteAllByPostingInAndAdminIsTrue(allPost);
        tagMappingRepository.flush();

        // fairView 매핑
        List<FairView> fairViews = fairViewRepository.findByPosts(allPost);
        Map<Long, List<FairView>> fairViewMap = fairViews.stream()
                .collect(Collectors.groupingBy(fv -> fv.getPosting()
                        .getId()));

        // 기본 태그 풀
        List<Tag> defaultTags = tagRepository.findAllByTagPoolIsNotNull();
        Map<String, Tag> tagMap = defaultTags.stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        // (postingId, tagId) 중복 방지
        Set<String> uniqueKeys = new HashSet<>();
        List<TagMapping> tagsToSave = new ArrayList<>();

        // user 태그 조회 → skip 기준
        List<TagMapping> userTags =
                tagMappingRepository.findAllByPostingInAndAdminIsFalseOrAdminIsNull(allPost);
        Set<String> userKeys = userTags.stream()
                .map(tm -> tm.getPosting()
                        .getId() + "_" + tm.getTag()
                        .getId())
                .collect(Collectors.toSet());
        for (Posting post : allPost) {
            StringBuilder postContent = new StringBuilder().append(post.getTitle())
                    .append("\n")
                    .append(post.getContent())
                    .append("\n");
            if (post.getPostType() == PostingType.FAIR_VIEW) {
                List<FairView> fairViewList = fairViewMap.get(post.getId());
                if (fairViewList != null) {
                    for (FairView fairView : fairViewList) {
                        postContent.append(fairView.getContent());
                    }
                }
            }
            for (Tag tag : examinePostingAndGetAdminTags(tagMap, postContent)) {
                if (tag == null) continue;
                String key = post.getId() + "_" + tag.getId();
                // user 태그와 겹치면 skip
                if (userKeys.contains(key)) continue;
                // admin 태그 중복 방지
                if (uniqueKeys.add(key)) {
                    tagsToSave.add(new TagMapping(post, tag, true));
                }
            }
            post.setAdminTagged(true);
        }
        tagMappingRepository.saveAll(tagsToSave);
    }

    private Set<Tag> examinePostingAndGetAdminTags(Map<String, Tag> tagMap,
            StringBuilder postContent) {
        if (postContent == null || postContent.isEmpty()) {
            return new HashSet<>();
        }
        String content = postContent.toString();
        Map<String, Double> tagScores = new HashMap<>();

        // 모든 태그에 대해 점수 계산
        for (Map.Entry<String, List<String>> category : TAG_POOL.entrySet()) {
            for (String tag : category.getValue()) {
                double score = calculateTagScore(content, tag);
                if (score > 0) {
                    tagScores.put(tag, score);
                }
            }
        }

        // 점수가 높은 순으로 정렬하여 상위 10개 선택
        List<String> result = tagScores.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        String lengthTag = generateLengthBasedTag(content);
        if (!result.contains(lengthTag)) {
            result.addFirst(lengthTag);
        }
        if (result.size() < 10) {
            result = addMeaningfulDefaultTags(result);
        }

        Set<Tag> tags = new HashSet<>();

        log.info("\nTag");
        for (String tagTitle : result) {
            tags.add(tagMap.get(tagTitle));
            log.info(tagTitle);
        }

        log.info("added\n");
        return tags;
    }

    private List<String> addMeaningfulDefaultTags(List<String> currentTags) {
        List<String> result = new ArrayList<>(currentTags);
        // 3. 기타 태그 추가 (마지막에)
        if (result.size() < 10 && !result.contains("기타")) {
            result.addFirst("기타");
        }
        if (result.size() < 10)
            // 맨 앞에 미분류 태그 추가
            if (!result.contains("미분류")) {
                result.addFirst("미분류");
            }
        return result;
    }

    private String generateLengthBasedTag(String content) {
        int length = content.length();

        if (length < 100) {
            return "단문";
        }
        else if (length < 300) {
            return "중문";
        }
        else if (length < 600) {
            return "장문";
        }
        else if (length < 1000) {
            return "초장문";
        }
        else {
            return "대하소설";
        }
    }

    /**
     * 특정 태그에 대한 점수 계산 (더 정교한 매칭)
     */
    private double calculateTagScore(String content, String tag) {
        double score = 0.0;
        String lowerContent = content.toLowerCase();

        // 1. 직접 매칭 (가장 높은 점수)
        if (lowerContent.contains(tag.toLowerCase())) {
            score += 3.0;
        }

        // 2. 키워드 매핑을 통한 매칭
        Set<String> keywords = KEYWORD_MAPPINGS.get(tag);
        if (keywords != null) {
            for (String keyword : keywords) {
                if (lowerContent.contains(keyword.toLowerCase())) {
                    score += 2.0;
                }
            }
        }

        // 3. 부분 매칭 (유사한 단어)
        String[] words = lowerContent.split("\\s+");
        for (String word : words) {
            if (isSimilarWord(word, tag)) {
                score += 1.0;
            }
        }

        // 4. 문맥적 매칭 (특정 패턴)
        score += calculateContextualScore(content, tag);

        return score;
    }

    /**
     * 단어 유사성 체크 (간단한 편집 거리 기반)
     */
    private boolean isSimilarWord(String word1, String word2) {
        if (word1.length() < 2 || word2.length() < 2) return false;

        // 포함 관계 체크
        if (word1.contains(word2) || word2.contains(word1)) {
            return true;
        }

        // 간단한 유사성 체크 (첫 글자와 마지막 글자 같은 경우)
        return word1.charAt(0) == word2.charAt(0) &&
                word1.charAt(word1.length() - 1) == word2.charAt(word2.length() - 1) &&
                Math.abs(word1.length() - word2.length()) <= 1;
    }

    /**
     * 문맥적 점수 계산
     */
    private double calculateContextualScore(String content, String tag) {
        double score = 0.0;
        String lower = content.toLowerCase();

        // 부정적 표현과 함께 나오는 감정 태그들
        if (isEmotionTag(tag)) {
            String[] negativeWords = {"안", "못", "없어", "싫어", "힘들어", "지쳐"};
            for (String negWord : negativeWords) {
                if (lower.contains(negWord)) {
                    score += 0.5;
                }
            }
        }


        return score;
    }

    private boolean isEmotionTag(String tag) {
        return TAG_POOL.get("감정표현")
                .contains(tag);
    }


    @Transactional
    public void updatePostDetailInfos() {
        // postview postlike comment pollvote 가 전날 0시 부터 생성괸 기록이 있는 모든 포스트,
        // 그리고 생성 혹은 수정 된 날짜가 전날 0시 이후인 모든 포스트가 대상
        List<PostDetailInfo> postDetailInfos = new ArrayList<>();

        List<Posting> newPosts = postRepository.findAllByCreatedAtAfterOrUpdatedAtAfter(
                LocalDateTime.now()
                        .minusDays(1), PostingType.ANNOUNCEMENT);
        List<Long> postIds = newPosts.stream()
                .distinct()
                .map(Posting::getId)
                .toList();
        List<PostDetailInfo> existingInfos = postDetailInfoRepository.findAllByPostIdIn(postIds);
        Set<Long> existingPostIds = existingInfos.stream()
                .map(PostDetailInfo::getPostId)
                .collect(Collectors.toSet());

        List<Long> userIds = newPosts.stream()
                .map(Posting::getUserId)
                .distinct()
                .toList();

        List<RedisDto.UserDto> userDtoList = cacheService.getMultipleUsersFromCache(userIds);
        Map<Long, RedisDto.UserDto> userDtoMap = userDtoList.stream()
                .filter(Objects::nonNull) // dto 자체가 null인지 체크
                .filter(dto -> dto.getUserId() != null) // userId null 체크
                .collect(Collectors.toMap(RedisDto.UserDto::getUserId, Function.identity()));
        List<TagMapping> tagMappings = tagMappingRepository.findByPostIdInIncludingAdmin(postIds);
        Map<Long, List<Long>> postUserTags = new HashMap<>();
        for (TagMapping tagMapping : tagMappings) {
            Long postId = tagMapping.getPosting()
                    .getId();
            postUserTags.computeIfAbsent(postId, k -> new ArrayList<>())
                    .add(tagMapping.getId());
        }
        for (Posting post : newPosts) {
            if (existingPostIds.contains(post.getId())) continue;
            PostDetailInfo item =
                    new PostDetailInfo(post, userDtoMap.getOrDefault(post.getUserId(), null),
                            postUserTags.getOrDefault(post.getId(), List.of()));
            postDetailInfos.add(item);

        }


        List<Long> oldPostIds = postRepository.findIdsByInteractedAtAfter(LocalDateTime.now()
                .minusDays(1), PostingType.ANNOUNCEMENT);
        List<PostDetailInfo> oldPostDetailInfos =
                postDetailInfoRepository.findAllByPostIdIn(oldPostIds);
        List<PostDetailInfo> allPostDetailInfos = new ArrayList<>();
        allPostDetailInfos.addAll(oldPostDetailInfos);
        allPostDetailInfos.addAll(postDetailInfos);
        Map<Long, PostDetailInfo> postDetailInfoMap = allPostDetailInfos.stream()
                .collect(Collectors.toMap(PostDetailInfo::getPostId,
                        postDetailInfo -> postDetailInfo));
        List<Long> postIdList = postDetailInfoMap.keySet()
                .stream()
                .distinct()
                .toList();

        List<PollVote> votes = voteRepository.findAllByPostingIdIn(postIdList);
        List<PostLike> likes = postLikeRepository.findAllByPostingIdIn(postIdList);
        List<PostView> views = postViewRepository.findAllByPostingIdIn(postIdList);
        Map<Long, Integer> voteMap = new HashMap<>();
        if (likes != null && !likes.isEmpty()) {
            for (PollVote vote : votes) {
                voteMap.merge(vote.getPollOption()
                        .getPoll()
                        .getPosting()
                        .getId(), 1, Integer::sum);
            }
        }
        Map<Long, Integer> likeMap = new HashMap<>();
        if (likes != null && !likes.isEmpty()) {
            for (PostLike like : likes) {
                likeMap.merge(like.getPosting()
                        .getId(), 1, Integer::sum);
            }
        }
        Map<Long, Integer> viewMap = new HashMap<>();
        if (views != null && !views.isEmpty()) {
            for (PostView view : views) {
                viewMap.merge(view.getPosting()
                        .getId(), 1, Integer::sum);
            }
        }
        Map<Long, Integer> commentMap = commentService.getPostCommentMapByPostIds(postIdList);
        for (PostDetailInfo postDetailInfo : allPostDetailInfos) {
            Long postId = postDetailInfo.getPostId();
            likeMap.putIfAbsent(postId, 0);
            viewMap.putIfAbsent(postId, 0);
            voteMap.putIfAbsent(postId, 0);
            commentMap.putIfAbsent(postId, 0);
            postDetailInfo.update(likeMap.getOrDefault(postId, 0), viewMap.getOrDefault(postId, 0),
                    voteMap.getOrDefault(postId, 0), commentMap.getOrDefault(postId, 0));
        }

        postDetailInfoRepository.saveAll(postDetailInfos);
    }

    @Transactional
    public Page<AdminDto.PostListItem> getAllDetailedPosts(int size, int page, String sortType,
            boolean asc, List<String> postTypes, List<Long> tags) {
        log.info("게시물 목록을 데이터베이스에서 조회합니다. (캐시 미적용)");

        List<PostingType> postingTypes = getFilteredPostTypeList(postTypes);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.fromString(asc ? "ASC" : "DESC"), sortType));

        Page<PostDetailInfo> postDetailInfos =
                postDetailInfoRepository.findAllContainsTagsFilteredByTypes(postingTypes, pageable);

        List<Long> postIds = postDetailInfos.stream()
                .map(PostDetailInfo::getPostId)
                .toList();
        List<Long> tagMappingIds = postDetailInfos.stream()
                .map(PostDetailInfo::getTagMappingIds)
                .flatMap(List::stream)
                .toList();
        List<TagMapping> tagMappings = tagMappingRepository.findAllByIdIn(tagMappingIds);
        Map<Long, List<TagMapping>> postTagMappingMap = tagMappings.stream()
                .collect(Collectors.groupingBy(tagMapping -> tagMapping.getPosting()
                        .getId()));
        List<RedisDto.PostDto> postDtoList = cacheService.getMultiplePostsFromCache(postIds);
        List<Long> userIds = postDtoList.stream()
                .map(RedisDto.PostDto::getUserId)
                .distinct()
                .toList();
        List<RedisDto.UserDto> userDtoList = cacheService.getMultipleUsersFromCache(userIds);
        Map<Long, RedisDto.UserDto> userDtoMap = new HashMap<>();
        Map<Long, RedisDto.PostDto> postDtoMap = new HashMap<>();
        for (RedisDto.PostDto postDto : postDtoList) {
            postDtoMap.put(postDto.getPostId(), postDto);
        }
        for (RedisDto.UserDto userDto : userDtoList) {
            userDtoMap.put(userDto.getUserId(), userDto);
        }

        List<AdminDto.PostListItem> postListItems = new ArrayList<>();
        for (PostDetailInfo postDetailInfo : postDetailInfos) {
            Long postId = postDetailInfo.getPostId();
            RedisDto.PostDto postDto = postDtoMap.get(postId);
            List<TagMapping> tagMappingForPost = postTagMappingMap.getOrDefault(postId, List.of());
            List<TagDto.TagMappingInfo> tagMappingInfos =
                    TagDto.TagMappingInfo.fromTagMappings(tagMappingForPost);
            postListItems.add(new AdminDto.PostListItem(postDetailInfo, postDto,
                    userDtoMap.get(postDto.getUserId()), tagMappingInfos));
        }
        return new PageImpl<>(postListItems, pageable, postDetailInfos.getTotalElements());
    }

    /**
     * 게시물 목록 캐시를 수동으로 무효화합니다.
     */
    @CacheEvict(value = "postDetailInfos", allEntries = true)
    public void clearPostListCache() {
        log.info("게시물 목록 캐시를 수동으로 무효화했습니다.");
    }

    public List<String> getSearchedTags(String keyword) {
        List<TagDto.Response> response = tagService.searchTags(keyword);
        List<String> adminTags = findTagsContainingKeyword(keyword);
        Set<String> tagNames = new HashSet<>(adminTags);

        tagNames.addAll(response.stream()
                .map(TagDto.Response::getName)
                .toList());

        return tagNames.stream()
                .toList();
    }

    public List<AdminDto.SearchedUser> createVirtualUsers(String keyword) {
        List<User> searchedUsers = keyword.isEmpty() ? userRepository.findUserByTestUserIsTrue() :
                userRepository.searchUsersByKeywordTestUserIsTrue(keyword);
        return AdminDto.SearchedUser.fromUserList(searchedUsers);
    }

}
