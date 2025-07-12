package com.may21.trobl.post.service;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.enums.TargetType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.comment.service.CommentService;
import com.may21.trobl.poll.domain.Poll;
import com.may21.trobl.poll.domain.PollOption;
import com.may21.trobl.poll.domain.PollVote;
import com.may21.trobl.poll.repository.PollOptionRepository;
import com.may21.trobl.poll.repository.PollRepository;
import com.may21.trobl.poll.repository.VoteRepository;
import com.may21.trobl.post.domain.*;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.report.ReportDto;
import com.may21.trobl.report.ReportService;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.service.TagService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostingServiceImpl implements PostingService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PollOptionRepository pollOptionRepository;
    private final FairViewRepository fairViewRepository;
    private final VoteRepository voteRepository;
    private final PostLikeRepository likeRepository;
    private final PostBookmarkRepository bookmarkRepository;
    private final PostViewRepository viewRepository;
    private final PollRepository pollRepository;
    private final TagService tagService;
    private final CommentService commentService;
    private final ReportService reportService;
    private final CacheManager cacheManager;

    @Override
    public Page<PostDto.ListItem> getPostsList(Pageable pageable, Long userId) {
        List<Long> blockedPostIds = reportService.getBlockedTargetIds(userId, TargetType.POSTING);
        List<Long> blockedUserIds = reportService.getBlockedTargetIds(userId, TargetType.USER);
        Page<Posting> posts = postRepository.findAllExceptBlocked(pageable, blockedPostIds, blockedUserIds);
        Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        List<Posting> postList = posts.stream().toList();
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts.stream().toList());
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, postList);
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId, postList);
        return posts.map(
                post -> {
                    Long postId = post.getId();
                    User owner = userMap.get(post.getUserId());
                    return new PostDto.ListItem(post, owner, likedPostIds.contains(postId), viewedPostIds.contains(postId), commentedPostIds.contains(postId));
                });
    }

    @Cacheable(value = "topPosts", key = "#type + '_' + (#userId != null ? #userId : 'anonymous')",
            condition = "#type != null")
    @Override
    public List<PostDto.Card> getTop10Views(String type, Long userId) {

        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        List<Long> blockedPostIds = userId != null ? reportService.getBlockedTargetIds(userId, TargetType.POSTING) : List.of();
        List<Posting> posts =
                switch (type.toLowerCase()) {
                    case "like" -> postRepository.findTopPostsByLikes(10, PostingType.POLL, blockedPostIds);
                    case "view" -> postRepository.findTopPostsByViews(10, PostingType.POLL, blockedPostIds);
                    case "share" -> postRepository.findTopPostsByShares(10, PostingType.POLL, blockedPostIds);
                    case "comment" -> postRepository.findTopPostsByComments(10, PostingType.POLL, blockedPostIds);
                    case "vote" -> postRepository.findTopPostsByVotes(10, blockedPostIds);
                    default ->
                            postRepository.findTopPostsByLikesAndViews(10, threeMonthsAgo, PostingType.POLL, blockedPostIds);
                };
        Map<Long, Integer> commentMaps = commentService.getPostCommentMap(posts);
        List<PostDto.Card> response = new ArrayList<>();
        for (Posting post : posts) {
            response.add(new PostDto.Card(post, commentMaps.get(post.getId())));
        }
        return response;
    }

    @Override
    public PostDto.Detail getPostDetail(Long postId, Long userId) {
        Posting post =
                postRepository
                        .findById(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        User owner =
                userRepository
                        .findById(post.getUserId())
                        .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        boolean isOwner = post.getUserId().equals(userId);
        Map<Long, User> userMap = new HashMap<>();
        boolean liked = false;
        boolean bookmarked = false;
        if (userId != null) {
            liked = likeRepository.existsByPostingIdAndUserId(postId, userId);
            bookmarked = bookmarkRepository.existsByPostingIdAndUserId(postId, userId);
            if (!viewRepository.existsByPostIdAndUserId(postId, userId)) {
                viewRepository.save(new PostView(post, userId));
            }
        }
        List<Tag> tags = tagService.getPostTags(post);
        List<Long> votedOptionIds = userId == null ? List.of() : voteRepository.findVotedPostByUserId(post, userId);
        return new PostDto.Detail(post, owner, userMap, tags, liked, bookmarked, votedOptionIds, isOwner);
    }

    @Override
    public PostDto.Detail createPost(PostDto.Request request, Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        PostingType postType =
                PostingType.valueOf(request.getPostType().toUpperCase());
        Posting post =
                Posting.builder()
                        .title(request.getTitle())
                        .postType(postType)
                        .content(request.getContent())
                        .userId(userId)
                        .nickname(user.getNickname())
                        .build();
        postRepository.save(post);
        if (postType == PostingType.POLL) {
            Poll poll = new Poll(request.getPollTitle(), post, request.isAllowMultipleVotes());
            pollRepository.save(poll);

            List<PostDto.PollItem> pollOptionsRequest = request.getPoll().getPollOptions();
            List<PollOption> pollOptions = new ArrayList<>();
            for (int i = 0; i < pollOptionsRequest.size(); i++) {
                PostDto.PollItem pollOptionRequest = pollOptionsRequest.get(i);
                PollOption pollOption =
                        PollOption.builder()
                                .name(pollOptionRequest.getName())
                                .poll(poll)
                                .index(i)
                                .build();
                pollOptions.add(pollOption);
            }
            pollOptionRepository.saveAll(pollOptions);
            poll.setPollOptions(pollOptions);
            post.setPoll(poll);
        } else if (postType == PostingType.FAIR_VIEW) {
            PostDto.OpinionItem opinionItem = request.getOptionItem();
            FairView fairView = FairView.builder()
                    .title(opinionItem.getTitle())
                    .content(opinionItem.getContent())
                    .post(post)
                    .userId(userId)
                    .build();
            fairViewRepository.save(fairView);
            post.addFairView(fairView);
        }
        Set<Tag> tags = tagService.createTags(request.getTags());
        List<TagMapping> tagResponses = tagService.createTagMapping(tags, post);
        post.setTags(tagResponses);
        Map<Long, User> userMap = new HashMap<>();
        userMap.put(userId, user);
        List<Tag> tagList = tags.stream().toList();
        return new PostDto.Detail(post, user, userMap, tagList, false, false, List.of(), true);
    }

    @Override
    public PostDto.Detail updatePost(PostDto.Request request, Long userId, Long postId) {
        Posting post =
                postRepository
                        .findById(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ExceptionCode.POST_NOT_AUTHORIZED);
        }
        post.update(request);
        if (post.getPostType() == PostingType.POLL && request.getPollId() != null) {
            Long pollId = request.getPollId();
            PostDto.PollDto pollDto = request.getPoll();
            String pollTitle = pollDto.getTitle();
            Boolean allowMultipleVotes = pollDto.isAllowMultipleVotes();
            Poll poll = pollRepository.findById(pollId).orElseThrow(() -> new BusinessException(ExceptionCode.POLL_NOT_FOUND));
            if (pollTitle != null && !pollTitle.equals(poll.getTitle())) {
                poll.setTitle(pollTitle);
            }
            if (!allowMultipleVotes.equals(poll.isAllowedMultipleVotes())) {
                poll.setAllowMultipleVotes(allowMultipleVotes);
            }
            List<PostDto.PollItem> pollOptionsRequest = pollDto.getPollOptions();
            updatePollOptions(pollOptionsRequest, poll);
        }
        Set<Tag> tags = tagService.createTags(request.getTags());
        List<TagMapping> tagResponses = tagService.updateTags(tags, post);
        post.getTags().clear();
        post.getTags().addAll(tagResponses);
        postRepository.save(post);
        List<Long> votedOptionIds = voteRepository.findVotedPostByUserId(post, userId);
        List<Tag> tagList = tags.stream().toList();
        Map<Long, User> userMap = new HashMap<>();
        return new PostDto.Detail(
                post,
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND)),
                userMap, tagList, false, false, votedOptionIds, true);
    }

    public List<PollOption> updatePollOptions(
            List<PostDto.PollItem> pollOptionsRequest, Poll poll) {
        List<PollOption> pollOptions = poll.getPollOptions();
        List<PollOption> updatedOptions = new ArrayList<>();
        List<Long> pollOptionIds =
                pollOptionsRequest.stream().map(PostDto.PollItem::getPollOptionId).toList();

        // Process poll options from the request
        for (int i = 0; i < pollOptionsRequest.size(); i++) {
            PostDto.PollItem pollOptionRequest = pollOptionsRequest.get(i);
            if (pollOptionRequest.getPollOptionId() == null) {
                // Create new poll option
                PollOption pollOption =
                        PollOption.builder()
                                .name(pollOptionRequest.getName())
                                .index(i)
                                .poll(poll)
                                .build();
                updatedOptions.add(pollOption);
            } else {
                // Update existing poll option
                PollOption existingOption =
                        pollOptions.stream()
                                .filter(option -> option.getId().equals(pollOptionRequest.getPollOptionId()))
                                .findFirst()
                                .orElse(
                                        new PollOption(
                                                pollOptionRequest.getName(), i, poll));
                if (pollOptionRequest.getName() != null && !pollOptionRequest.getName().equals(existingOption.getName()))
                    existingOption.setName(pollOptionRequest.getName());
                existingOption.setIndex(i);
                updatedOptions.add(existingOption);
            }
        }
        // Find and remove poll options that are not in the request
        List<PollOption> optionsToRemove =
                pollOptions.stream().filter(option -> !pollOptionIds.contains(option.getId())).toList();

        // Remove the options that are not in the request
        if (!optionsToRemove.isEmpty()) {
            pollOptions.removeAll(optionsToRemove);
            pollOptionRepository.deleteAll(optionsToRemove);
        }

        // Add new poll options to the post
        if (!updatedOptions.isEmpty()) {
            pollOptionRepository.saveAll(updatedOptions);
        }
        poll.setPollOptions(updatedOptions);
        return updatedOptions;
    }

    @Override
    public boolean votePoll(Long pollOptionId, Long userId) {
        PollVote pollVote =
                voteRepository.findByPollOptionIdAndUserId(pollOptionId, userId).orElse(null);
        if (pollVote == null) {
            PollOption pollOption =
                    pollOptionRepository
                            .findById(pollOptionId)
                            .orElseThrow(() -> new BusinessException(ExceptionCode.POLL_OPTION_NOT_FOUND));
            pollVote = new PollVote(pollOption, userId);
            voteRepository.save(pollVote);
            return true;
        }
        voteRepository.deleteByPollOptionIdAndUserId(pollOptionId, userId);
        return false;
    }

    @Override
    public boolean sharePost(Long postId, Long id) {
        Posting post =
                postRepository
                        .findById(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        post.incrementShareCount();
        return true;
    }

    @Override
    public boolean viewPost(Long postId, Long id) {
        Posting post =
                postRepository
                        .findById(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        post.incrementViewCount();
        return true;
    }

    @Override
    public PostDto.ListItem likePost(Long postId, Long userId) {
        boolean liked = true;
        PostLike postLike = likeRepository.findByPostingIdAndUserId(postId, userId).orElse(null);
        Posting post =
                postRepository
                        .findById(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (postLike == null) {
            postLike = new PostLike(post, userId);
            likeRepository.save(postLike);
        } else {
            liked = false;
            likeRepository.deleteByEntity(postLike);
        }
        boolean commented = commentService.existsByPostIdAndUserId(postId, userId);
        return new PostDto.ListItem(post, null, liked, true, commented);
    }

    @Override
    public boolean deletePost(Long userId, Long postId) {
        Posting post =
                postRepository
                        .findById(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ExceptionCode.POST_NOT_AUTHORIZED);
        }
        postRepository.delete(post);
        return true;
    }

    @Override
    public Page<PostDto.ListItem> getMyPosts(Long userId, int page, int size) {
        Page<Posting> posts = postRepository.findByUserId(userId, PageRequest.of(page, size));
        Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        List<Posting> postList = posts.stream().toList();
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts.stream().toList());
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, postList);
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId, postList);
        List<PostDto.ListItem> response = new ArrayList<>();
        for (Posting post : posts) {
            response.add(new PostDto.ListItem(post, userMap.get(post.getUserId()), likedPostIds.contains(post.getId()), viewedPostIds.contains(post.getId()), commentedPostIds.contains(post.getId())));
        }
        return new PageImpl<>(response, PageRequest.of(page, size), posts.getTotalElements());
    }

    @Override
    public Page<PostDto.ListItem> getLikedPosts(Long userId, int page, int size) {
        Page<Posting> posts = likeRepository.findPostingByUserId(userId, PageRequest.of(page, size));
        Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        List<Posting> postList = posts.stream().toList();
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, postList);
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId, postList);
        return posts.map(
                post -> {
                    User user = userMap.get(post.getUserId());
                    return new PostDto.ListItem(post, user, true, viewedPostIds.contains(post.getId()), commentedPostIds.contains(post.getId()));
                });
    }

    @Override
    public Page<PostDto.ListItem> getVisitedPosts(Long userId, int page, int size) {
        Page<Posting> posts = viewRepository.findPostingByUserId(userId, PageRequest.of(page, size));
        Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts.stream().toList());
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId, posts.stream().toList());
        return posts.map(
                post -> {
                    User user = userMap.get(post.getUserId());
                    return new PostDto.ListItem(post, user, likedPostIds.contains(post.getId()), true, commentedPostIds.contains(post.getId()));
                });
    }


    @Override
    public PostDto.Detail addPairView(Long postId, Long userId, PostDto.OpinionItem opinionItem) {
        Posting post =
                postRepository
                        .getPostWithOnePairView(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.FAIR_VIEW_CAN_NOT_BE_ADDED));
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        FairView fairView = FairView.builder()
                .title(opinionItem.getTitle())
                .content(opinionItem.getContent())
                .post(post)
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();
        fairViewRepository.save(fairView);
        Map<Long, User> userMap = new HashMap<>();
        userMap.put(userId, user);
        post.addFairView(fairView);

        return new PostDto.Detail(post, user, userMap, List.of(), false, false, List.of(), true);
    }

    @Override
    public List<PostDto.QuickPoll> getRandomQuickPoll(Long userId) {
        List<Long> blockedPostIds = userId != null ? reportService.getBlockedTargetIds(userId, TargetType.POSTING) : List.of();
        
        // ✅ 1번째 쿼리: Native Query로 랜덤 Posting 조회 (안전하게 RANDOM() 사용)
        List<Posting> posts = postRepository.findRandomPostsByType(5, PostingType.POLL, blockedPostIds);
        
        if (posts.isEmpty()) {
            return List.of();
        }
        
        // ✅ 2번째 쿼리: Poll 정보를 별도로 조회 (여러 개 Posting에 대해 한 번에)
        List<Long> postIds = posts.stream().map(Posting::getId).toList();
        postRepository.findPostsWithPollByIds(postIds); // 이는 별도로 만들어야 함
        
        // ✅ 3번째 쿼리: 투표 정보를 한 번의 쿼리로 조회
        List<Long> votedOptionIds = userId != null ? 
            voteRepository.findVotedOptionIdsByUserId(posts, userId) : List.of();
        
        // ✅ 스트림으로 처리하여 추가 쿼리 방지
        return posts.stream()
                .map(post -> {
                    boolean isOwner = userId != null && post.getUserId().equals(userId);
                    return new PostDto.QuickPoll(post, votedOptionIds, isOwner);
                })
                .toList();
    }

    @Override
    public boolean bookmarkPost(Long postId, Long userId) {
        boolean marked = true;
        PostBookmark bookmark = bookmarkRepository.findByPostingIdAndUserId(postId, userId).orElse(null);
        if (bookmark == null) {
            Posting post =
                    postRepository
                            .findById(postId)
                            .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
            bookmark = new PostBookmark(post, userId);
            bookmarkRepository.save(bookmark);
        } else {
            marked = false;
            bookmarkRepository.delete(bookmark);
        }
        return marked;
    }

    @Override
    public Page<PostDto.ListItem> getBookmarkedPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Posting> postPages = bookmarkRepository.findPostsByUserId(userId, pageable);
        Set<Long> userIds = postPages.stream().map(Posting::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Posting> posts = postPages.stream().toList();
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts);
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, posts);
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId, posts);
        return postPages.map(
                post -> {
                    User user = userMap.get(post.getUserId());
                    return new PostDto.ListItem(post, user, likedPostIds.contains(post.getId()), viewedPostIds.contains(post.getId()), commentedPostIds.contains(post.getId()));
                });
    }

    @Override
    public Page<PostDto.ListItem> getVotedPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Posting> postPages = voteRepository.findVotedPostsByUserId(userId, pageable);
        Set<Long> userIds = postPages.stream().map(Posting::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Posting> posts = postPages.stream().toList();
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts);
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, posts);
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId, posts);
        return postPages.map(
                post -> {
                    User user = userMap.get(post.getUserId());
                    return new PostDto.ListItem(post, user, likedPostIds.contains(post.getId()), viewedPostIds.contains(post.getId()), commentedPostIds.contains(post.getId()));
                });
    }

    @Override
    public Page<PostDto.ListItem> getFairViewConfirmList(Long userId, Pageable pageable) {
        List<User> users =
                userRepository
                        .findPartnerAndUserById(userId);
        if (users.isEmpty()) {
            throw new BusinessException(ExceptionCode.USER_NOT_FOUND);
        }
        List<Long> userIds = users.stream().map(User::getId).toList();
        Page<Posting> postPages = postRepository.findAllUnconfirmedPostsByUserIdIn(userIds, pageable);
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        List<Posting> posts = postPages.stream().toList();
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts);
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, posts);
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId, posts);
        return postPages.map(
                post -> {
                    User user = userMap.get(post.getUserId());
                    return new PostDto.ListItem(post, user, likedPostIds.contains(post.getId()), viewedPostIds.contains(post.getId()), commentedPostIds.contains(post.getId()));
                });
    }

    @Override
    public boolean confirmFairViewPost(Long userId, Long postId) {
        Posting post =
                postRepository
                        .findById(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (post.getPostType() != PostingType.FAIR_VIEW) {
            throw new BusinessException(ExceptionCode.POST_NOT_FIT_FOR_CONFIRMATION);
        }
        post.setConfirmed(true);
        return true;
    }

    @Override
    public boolean confirmFairView(Long userId, Long fairViewId) {
        FairView fairView =
                fairViewRepository
                        .findById(fairViewId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));

        fairView.setConfirmed(true);
        return true;
    }

    @Override
    public List<PostDto.ListItem> searchPostsByKeyword(Long userId, String keyword) {

        List<Long> blockedPostIds = reportService.getBlockedTargetIds(userId, TargetType.POSTING);
        List<Long> blockedUserIds = reportService.getBlockedTargetIds(userId, TargetType.USER);
        List<Posting> posts = postRepository.searchByKeyword(keyword, blockedPostIds, blockedUserIds);

        if (posts.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        // if userId is null, return posts without user information
        List<Long> likedPostIds = userId == null ? List.of() : postRepository.getAllIdsInListLikedByUserId(userId, posts);
        List<Long> viewedPostIds = userId == null ? List.of() : postRepository.getAllIdsInListViewedByUserId(userId, posts);

        List<Long> commentedPostIds = userId == null ? List.of() : postRepository.getAllIdsInListCommentedByUserId(userId, posts);
        List<PostDto.ListItem> response = new ArrayList<>();
        for (Posting post : posts) {
            User user = userMap.get(post.getUserId());
            response.add(new PostDto.ListItem(post, user, likedPostIds.contains(post.getId()), viewedPostIds.contains(post.getId()), commentedPostIds.contains(post.getId())));
        }
        if (!response.isEmpty()) {
            return response;
        }
        return List.of();
    }

    @Override
    public void setNickname(Long userId, String nickname) {
        List<Posting> posts = postRepository.findAllByUserId(userId);
        for (Posting post : posts) {
            post.setNickname(nickname);
        }

    }

    @CacheEvict(value = "topPosts", allEntries = true)
    @Override
    public void evictAllTopPosts() {
        log.info("Evicting all cached top posts");
        // This method will clear the cache for all top posts
    }

    @Override
    public boolean reportPost(Long userId, Long postId, ReportDto.Request reportRequest) {
        Posting post =
                postRepository
                        .findById(postId)
                        .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        int reportedCount = reportService.report(userId, postId, TargetType.POSTING, reportRequest);
        if (reportedCount >= 10) {
            post.setReported(true);
        }
        evictTopPostsCache(userId);
        return true;
    }


    private void evictTopPostsCache(Long userId) {
        Cache cache = cacheManager.getCache("topPosts");
        if (cache == null || userId == null) return;

        List<String> types = List.of("like", "view", "share", "comment", "vote");
        String userKey = userId.toString();

        for (String type : types) {
            String key = type + "_" + userKey;
            cache.evictIfPresent(key);
        }
    }
}
