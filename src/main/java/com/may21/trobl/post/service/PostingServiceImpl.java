package com.may21.trobl.post.service;

import com.may21.trobl._global.enums.ItemType;
import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.utility.ProfanityFilter;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.bookmark.PostBookmark;
import com.may21.trobl.bookmark.PostBookmarkRepository;
import com.may21.trobl.comment.service.CommentService;
import com.may21.trobl.notification.domain.ContentUpdateService;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.poll.domain.Poll;
import com.may21.trobl.poll.domain.PollOption;
import com.may21.trobl.poll.domain.PollVote;
import com.may21.trobl.poll.repository.PollOptionRepository;
import com.may21.trobl.poll.repository.PollRepository;
import com.may21.trobl.poll.repository.VoteRepository;
import com.may21.trobl.post.domain.*;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.redis.CacheService;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.report.ReportDto;
import com.may21.trobl.report.ReportService;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.service.TagService;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
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
    private final NotificationService notificationService;
    private final ContentUpdateService contentUpdateService;
    private final ProfanityFilter profanityFilter;
    private final CacheService cacheService;
    private final PostViewRepository postViewRepository;

    @Override
    public Page<PostDto.ListItem> getPostsList(Pageable pageable, Long userId) {
        List<Long> blockedPostIds = reportService.getBlockedTargetIds(userId, ItemType.POST);
        List<Long> blockedUserIds = reportService.getBlockedTargetIds(userId, ItemType.USER);
        Page<Posting> posts =
                postRepository.findAllExceptBlocked(pageable, blockedPostIds, blockedUserIds);
        Set<Long> userIds = posts.stream()
                .map(Posting::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        List<Posting> postList = posts.stream()
                .toList();
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts.stream()
                .toList());
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, postList);
        List<Long> commentedPostIds =
                postRepository.getAllIdsInListCommentedByUserId(userId, postList);
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(postList);
        return posts.map(post -> {
            Long postId = post.getId();
            User owner = userMap.get(post.getUserId());
            return new PostDto.ListItem(post, owner, tagMap.getOrDefault(postId, List.of()),
                    likedPostIds.contains(postId), viewedPostIds.contains(postId),
                    commentedPostIds.contains(postId));
        });
    }

    @Cacheable(value = "topPosts", key = "#type + '_' + (#userId != null ? #userId : 'anonymous')", condition = "#type != null")
    @Override
    public List<PostDto.Card> getTop10Views(String type, Long userId) {

        LocalDate threeMonthsAgo = LocalDate.now()
                .minusMonths(3);
        List<Long> blockedPostIds =
                userId != null ? reportService.getBlockedTargetIds(userId, ItemType.POST) :
                        List.of();
        List<Posting> posts = switch (type.toLowerCase()) {
            case "like" -> postRepository.findTopPostsByLikes(10, PostingType.POLL, blockedPostIds);
            case "view" -> postRepository.findTopPostsByViews(10, PostingType.POLL, blockedPostIds);
            case "share" ->
                    postRepository.findTopPostsByShares(10, PostingType.POLL, blockedPostIds);
            case "comment" ->
                    postRepository.findTopPostsByComments(10, PostingType.POLL, blockedPostIds);
            case "vote" -> postRepository.findTopPostsByVotes(10, blockedPostIds);
            default ->
                    postRepository.findTopPostsByLikesAndViews(10, threeMonthsAgo, PostingType.POLL,
                            blockedPostIds);
        };
        Map<Long, Integer> commentMaps = commentService.getPostCommentMap(posts);
        Map<Long, User> userMap = new HashMap<>();
        if (userId != null) {
            userMap = userRepository.findAllById(posts.stream()
                            .map(Posting::getUserId)
                            .collect(Collectors.toSet()))
                    .stream()
                    .collect(Collectors.toMap(User::getId, Function.identity()));
        }
        List<PostDto.Card> response = new ArrayList<>();
        for (Posting post : posts) {
            response.add(new PostDto.Card(post, commentMaps.get(post.getId()),
                    userMap.getOrDefault(post.getUserId(), null)));
        }
        return response;
    }

    @Override
    public PostDto.Detail getPostDetail(Long postId, Long userId) {
        RedisDto.PostDto postDto = cacheService.getPostFromCache(postId);
        Long ownerId = postDto.getUserId();
        RedisDto.UserDto owner = cacheService.getUserFromCache(ownerId);
        boolean isOwner = ownerId.equals(userId);
        Map<Long, RedisDto.UserDto> userMap = new HashMap<>();
        userMap.put(postDto.getUserId(), owner);
        List<RedisDto.FairViewDto> fairViews = null;
        RedisDto.PollDto pollDto = null;
        List<RedisDto.PollOptionDto> optionDtoList = null;
        if (postDto.getPostType() == PostingType.FAIR_VIEW) {
            fairViews = cacheService.getFairViewFromCache(postId);
            for (RedisDto.FairViewDto fairView : fairViews) {
                Long fairviewId = fairView.getUserId();
                if (!Objects.equals(fairviewId, ownerId)) {
                    RedisDto.UserDto partner = cacheService.getUserFromCache(fairviewId);
                    userMap.put(partner.getUserId(), partner);
                }
            }
        }else if (postDto.getPostType() == PostingType.POLL) {
            pollDto = cacheService.getPollFromCache(postId);
            optionDtoList =
                    cacheService.getPollOptionFromCache(postId);
        }
        boolean liked = false;
        boolean bookmarked = false;
        if (userId != null) {
            liked = likeRepository.existsByPostingIdAndUserId(postId, userId);
            bookmarked = bookmarkRepository.existsByPostingIdAndUserId(postId, userId);
            Posting post =
                    postRepository.findById(postId).orElseThrow(()->new BusinessException(ExceptionCode.POST_NOT_FOUND));
            if (!viewRepository.existsByPostIdAndUserId(postId, userId)) {
                viewRepository.save(new PostView(post, userId));
            }
        }
        List<Tag> tags = tagService.getPostTags(postId);
        List<Long> votedOptionIds =
                userId == null ? List.of() : voteRepository.findVotedPostIdByUserId(postId, userId);
        PostDto.Detail detailDto =
                new PostDto.Detail(postDto, fairViews, pollDto,optionDtoList, userMap, tags, liked,
                        bookmarked,
                        votedOptionIds,
                        isOwner);
        if (!postDto.isConfirmed() && postDto.getPostType() == PostingType.FAIR_VIEW)
            detailDto.blindPartnerContent(userId);
        return detailDto;
    }

    @Override
    public PostDto.Detail createPost(PostDto.Request request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        PostingType postType = PostingType.valueOf(request.getPostType()
                .toUpperCase());
        Posting post = Posting.builder()
                .title(profanityFilter.filterProfanity(request.getTitle()))
                .postType(postType)
                .content(profanityFilter.filterProfanity(request.getContent()))
                .userId(userId)
                .build();
        postRepository.save(post);
        if (postType == PostingType.POLL) {
            Poll poll = new Poll(profanityFilter.filterProfanity(request.getPollTitle()), post,
                    request.isAllowMultipleVotes());
            pollRepository.save(poll);

            List<PostDto.PollOptionRequest> pollOptionsRequest = request.getPoll()
                    .getPollOptions();
            List<PollOption> pollOptions = new ArrayList<>();
            for (int i = 0; i < pollOptionsRequest.size(); i++) {
                PostDto.PollOptionRequest pollOptionRequest = pollOptionsRequest.get(i);
                PollOption pollOption = PollOption.builder()
                        .name(profanityFilter.filterProfanity(pollOptionRequest.getName()))
                        .poll(poll)
                        .index(i)
                        .build();
                pollOptions.add(pollOption);
            }
            pollOptionRepository.saveAll(pollOptions);
            poll.setPollOptions(pollOptions);
            post.setPoll(poll);
        }
        else if (postType == PostingType.FAIR_VIEW) {
            Long partnerId = user.getPartnerId();
            if (partnerId == null) {
                throw new BusinessException(ExceptionCode.PARTNER_NOT_FOUND);
            }
            User partner = userRepository.findPartnerById(partnerId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.PARTNER_NOT_FOUND));
            PostDto.FairViewItem fairView = request.getFairViewItem();
            List<FairView> fairViews = new ArrayList<>();
            FairView myFairView = FairView.builder()
                    .title(fairView.getTitle())
                    .content(profanityFilter.filterProfanity(fairView.getContent()))
                    .post(post)
                    .userId(userId)
                    .nickname(user.getNickname())
                    .build();
            myFairView.setConfirmed(true);
            FairView partnerFairView = new FairView(partner, post);
            notificationService.sendFairViewRequest(post.getId(), partner);
            contentUpdateService.fairViewRequestUpdate(post.getId(), partner.getId());

            fairViews.add(partnerFairView);
            fairViews.add(myFairView);
            fairViewRepository.saveAll(fairViews);
            post.addFairView(myFairView);
            post.addFairView(partnerFairView);
        }
        Set<Tag> tags = tagService.createTags(request.getTags());
        List<TagMapping> tagResponses = tagService.createTagMapping(tags, post);
        post.setTags(tagResponses);
        Map<Long, User> userMap = new HashMap<>();
        userMap.put(userId, user);
        List<Tag> tagList = tags.stream()
                .toList();
        return new PostDto.Detail(post, user, userMap, tagList, false, false, List.of(), true);
    }

    @Override
    public PostDto.Detail updatePost(PostDto.Request request, Long userId, Long postId) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (!post.getUserId()
                .equals(userId)) {
            throw new BusinessException(ExceptionCode.POST_NOT_AUTHORIZED);
        }
        post.update(request);
        if (post.getPostType() == PostingType.POLL && request.getPollId() != null) {
            Long pollId = request.getPollId();
            PostDto.PollRequest pollDto = request.getPoll();
            String pollTitle = profanityFilter.filterProfanity(pollDto.getTitle());
            Boolean allowMultipleVotes = pollDto.isAllowMultipleVotes();
            Poll poll = pollRepository.findById(pollId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.POLL_NOT_FOUND));
            if (pollTitle != null && !pollTitle.equals(poll.getTitle())) {
                poll.setTitle(pollTitle);
            }
            if (!allowMultipleVotes.equals(poll.isAllowedMultipleVotes())) {
                poll.setAllowMultipleVotes(allowMultipleVotes);
            }
            List<PostDto.PollOptionRequest> pollOptionsRequest = pollDto.getPollOptions();
            updatePollOptions(pollOptionsRequest, poll);
        }
        Set<Tag> tags = tagService.createTags(request.getTags());
        List<TagMapping> tagResponses = tagService.updateTags(tags, post);
        post.getTags()
                .clear();
        post.getTags()
                .addAll(tagResponses);
        postRepository.save(post);
        List<Long> votedOptionIds = voteRepository.findVotedPostIdByUserId(postId, userId);
        List<Tag> tagList = tags.stream()
                .toList();
        Map<Long, User> userMap = new HashMap<>();
        return new PostDto.Detail(post, userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND)), userMap,
                tagList, false, false, votedOptionIds, true);
    }

    public List<PollOption> updatePollOptions(List<PostDto.PollOptionRequest> pollOptionsRequest,
            Poll poll) {
        List<PollOption> pollOptions = poll.getPollOptions();
        List<PollOption> updatedOptions = new ArrayList<>();
        List<Long> pollOptionIds = pollOptionsRequest.stream()
                .map(com.may21.trobl.post.dto.PostDto.PollOptionRequest::getPollOptionId)
                .toList();

        // Process poll options from the request
        for (int i = 0; i < pollOptionsRequest.size(); i++) {
            PostDto.PollOptionRequest pollOptionRequest = pollOptionsRequest.get(i);
            if (pollOptionRequest.getPollOptionId() == null) {
                // Create new poll option
                String filteredName = profanityFilter.filterProfanity(pollOptionRequest.getName());
                PollOption pollOption = PollOption.builder()
                        .name(filteredName)
                        .index(i)
                        .poll(poll)
                        .build();
                updatedOptions.add(pollOption);
            }
            else {
                // Update existing poll option
                PollOption existingOption = pollOptions.stream()
                        .filter(option -> option.getId()
                                .equals(pollOptionRequest.getPollOptionId()))
                        .findFirst()
                        .orElse(new PollOption(pollOptionRequest.getName(), i, poll));
                if (pollOptionRequest.getName() != null && !pollOptionRequest.getName()
                        .equals(existingOption.getName())) {
                    String filteredName =
                            profanityFilter.filterProfanity(pollOptionRequest.getName());
                    existingOption.setName(filteredName);
                }
                existingOption.setIndex(i);
                updatedOptions.add(existingOption);
            }
        }
        // Find and remove poll options that are not in the request
        List<PollOption> optionsToRemove = pollOptions.stream()
                .filter(option -> !pollOptionIds.contains(option.getId()))
                .toList();

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
        PollVote pollVote = voteRepository.findByPollOptionIdAndUserId(pollOptionId, userId)
                .orElse(null);
        if (pollVote == null) {
            PollOption pollOption = pollOptionRepository.findById(pollOptionId)
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
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        post.incrementShareCount();
        return true;
    }

    @Override
    public boolean viewPost(Long postId, Long id) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        post.incrementViewCount();
        return true;
    }

    @Override
    public PostDto.ListItem likePost(Long postId, Long userId) {
        boolean liked = true;
        PostLike postLike = likeRepository.findByPostingIdAndUserId(postId, userId)
                .orElse(null);
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (postLike == null) {
            postLike = new PostLike(post, userId);
            likeRepository.save(postLike);
        }
        else {
            liked = false;
            likeRepository.deleteByEntity(postLike);
        }
        boolean commented = commentService.existsByPostIdAndUserId(postId, userId);
        List<Tag> tags = tagService.getPostTags(postId);
        return new PostDto.ListItem(post, null, tags, liked, true, commented);
    }

    @Override
    public boolean deletePost(Long userId, Long postId) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (!post.getUserId()
                .equals(userId)) {
            throw new BusinessException(ExceptionCode.POST_NOT_AUTHORIZED);
        }
        postRepository.delete(post);
        evictAllTopPosts();
        return true;
    }

    @Override
    public Page<PostDto.MyListItem> getMyPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt")
                .descending());
        Page<Posting> posts = postRepository.findByUserId(userId, pageable);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        List<Posting> postList = posts.stream()
                .toList();
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts.stream()
                .toList());
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, postList);
        List<Long> commentedPostIds =
                postRepository.getAllIdsInListCommentedByUserId(userId, postList);
        Map<Long, NotificationDto.ContentUpdateStatus> contentUpdates =
                contentUpdateService.getContentUpdatesByUserId(userId, postList, Posting::getId,
                        ItemType.POST);
        List<PostDto.MyListItem> response = new ArrayList<>();
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(postList);
        for (Posting post : posts) {
            response.add(
                    new PostDto.MyListItem(post, user, tagMap.getOrDefault(post.getId(), List.of()),
                            likedPostIds.contains(post.getId()),
                            viewedPostIds.contains(post.getId()),
                            commentedPostIds.contains(post.getId()),
                            contentUpdates.get(post.getId())));
        }
        return new PageImpl<>(response, PageRequest.of(page, size), posts.getTotalElements());
    }

    @Override
    public Page<PostDto.ListItem> getLikedPosts(Long userId, int page, int size) {
        Page<Posting> posts =
                likeRepository.findPostingByUserId(userId, PageRequest.of(page, size));
        Set<Long> userIds = posts.stream()
                .map(Posting::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        List<Posting> postList = posts.stream()
                .toList();
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, postList);
        List<Long> commentedPostIds =
                postRepository.getAllIdsInListCommentedByUserId(userId, postList);
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(postList);
        return posts.map(post -> {
            User user = userMap.get(post.getUserId());
            return new PostDto.ListItem(post, user, tagMap.getOrDefault(post.getId(), List.of()),
                    true, viewedPostIds.contains(post.getId()),
                    commentedPostIds.contains(post.getId()));
        });
    }

    @Override
    public Page<PostDto.ListItem> getVisitedPosts(Long userId, int page, int size) {
        Page<Posting> posts =
                viewRepository.findPostingByUserId(userId, PageRequest.of(page, size));
        Set<Long> userIds = posts.stream()
                .map(Posting::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts.stream()
                .toList());
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId,
                posts.stream()
                        .toList());
        List<Posting> postList = posts.stream()
                .toList();
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(postList);
        return posts.map(post -> {
            User user = userMap.get(post.getUserId());
            return new PostDto.ListItem(post, user, tagMap.getOrDefault(post.getId(), List.of()),
                    likedPostIds.contains(post.getId()), true,
                    commentedPostIds.contains(post.getId()));
        });
    }


    @Override
    public PostDto.FairViewItem setFairView(Long fairViewId, Long userId,
            PostDto.FairViewRequest request) {
        FairView fairView = fairViewRepository.findById(fairViewId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.FAIR_VIEW_NOT_FOUND));
        if (!fairView.getUserId()
                .equals(userId)) {
            throw new BusinessException(ExceptionCode.UNAUTHORIZED);
        }
        String title =
                fairView.getTitle() != null ? profanityFilter.filterProfanity(fairView.getTitle()) :
                        "";
        String content = fairView.getContent() != null ?
                profanityFilter.filterProfanity(fairView.getContent()) : "";
        fairView.setTitle(title);
        fairView.setContent(content);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        if (!fairView.isConfirmed()) {
            fairView.setConfirmed(true);
            notificationService.sendFairViewConfirmedRequest(fairView.getId());
        }
        return new PostDto.FairViewItem(fairView, user);
    }

    @Override
    public List<PostDto.QuickPoll> getRandomQuickPoll(Long userId) {
        List<Long> blockedPostIds =
                userId != null ? reportService.getBlockedTargetIds(userId, ItemType.POST) :
                        List.of();

        List<Posting> posts =
                postRepository.findRandomPostsByType(5, PostingType.POLL, blockedPostIds);

        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> votedOptionIds =
                userId != null ? voteRepository.findVotedOptionIdsByUserId(posts, userId) :
                        List.of();

        List<PostView> views = postViewRepository.findAllByPosts(posts);
        Map<Long, Integer> postViewCOuntMap = getViewMaps(views, posts);
        return cacheService.getQuickPollDtoByCached(userId, posts, votedOptionIds, postViewCOuntMap);
    }

    private Map<Long, Integer> getViewMaps(List<PostView> views, List<Posting> posts) {
        Map<Long, Integer> postViewCountMap = new HashMap<>();
        for (PostView view : views) {
            postViewCountMap.put(view.getPosting()
                    .getId(), postViewCountMap.getOrDefault(view.getPosting()
                            .getId(), 0) + 1);
        }
        for (Posting post : posts) {
            if (!postViewCountMap.containsKey(post.getId())) {
                postViewCountMap.put(post.getId(), 0);
            }
        }
        return postViewCountMap;
    }

    @Override
    public List<PostDto.QuickPoll> getQuickPolls(Long userId) {
        List<Long> blockedPostIds =
                userId != null ? reportService.getBlockedTargetIds(userId, ItemType.POST) :
                        List.of();
        List<Long> blockedUserIds =
                userId != null ? reportService.getBlockedTargetIds(userId, ItemType.USER) :
                        List.of();

        // Fetch more posts than needed to have enough after prioritization
        List<Posting> allRecentPolls =
                postRepository.findRecentPollsByType(15, PostingType.POLL, blockedPostIds,
                        blockedUserIds);

        if (allRecentPolls.isEmpty()) {
            return List.of();
        }

        // Get posts that user has already viewed
        List<Long> viewedPostIds = userId != null ?
                postRepository.getAllIdsInListViewedByUserId(userId, allRecentPolls) : List.of();

        // Separate viewed and unviewed posts
        List<Posting> unviewedPosts = allRecentPolls.stream()
                .filter(post -> !viewedPostIds.contains(post.getId()))
                .toList();

        List<Posting> viewedPosts = allRecentPolls.stream()
                .filter(post -> viewedPostIds.contains(post.getId()))
                .toList();

        // Prioritize unviewed posts first, then viewed posts
        List<Posting> prioritizedPosts = new ArrayList<>();
        prioritizedPosts.addAll(unviewedPosts);
        prioritizedPosts.addAll(viewedPosts);

        // Take only the first 5 posts after prioritization
        List<Posting> finalPosts = prioritizedPosts.stream()
                .limit(5)
                .toList();

        if (finalPosts.isEmpty()) {
            return List.of();
        }
        // Build QuickPoll list using cached data when available

        List<Long> votedOptionIds = voteRepository.findVotedOptionIdsByUserId(finalPosts, userId);
        List<Long> userIds = finalPosts.stream()
                .map(Posting::getUserId)
                .distinct()
                .toList();
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));


        List<PostDto.QuickPoll> quickPolls = new ArrayList<>();
        for (Posting post : finalPosts) {
            Long postOwnerId = post.getUserId();
            PostDto.QuickPoll quickPoll =
                    new PostDto.QuickPoll(post, userMap.getOrDefault(postOwnerId, null),
                            votedOptionIds, postOwnerId == userId);
            quickPolls.add(quickPoll);
        }


        // Maintain the original order based on prioritization
        return quickPolls.stream()
                .sorted((a, b) -> {
                    int aIndex = finalPosts.stream()
                            .mapToInt(p -> p.getId()
                                    .equals(a.getPostId()) ? finalPosts.indexOf(p) : -1)
                            .filter(i -> i >= 0)
                            .findFirst()
                            .orElse(Integer.MAX_VALUE);
                    int bIndex = finalPosts.stream()
                            .mapToInt(p -> p.getId()
                                    .equals(b.getPostId()) ? finalPosts.indexOf(p) : -1)
                            .filter(i -> i >= 0)
                            .findFirst()
                            .orElse(Integer.MAX_VALUE);
                    return Integer.compare(aIndex, bIndex);
                })
                .toList();
    }

    @Override
    public PostDto.FairViewItem updateVirtualFairView(Long fairViewId,
            PostDto.FairViewRequest request) {
        FairView fairView = fairViewRepository.findById(fairViewId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.FAIR_VIEW_NOT_FOUND));
        fairView.update(request);
        User user = userRepository.findById(fairView.getUserId())
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        return new PostDto.FairViewItem(fairView, user);
    }

    @Override
    public PostDto.PollDto updatePoll(Long pollId, PostDto.PollRequest request) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POLL_NOT_FOUND));
        String pollTitle = profanityFilter.filterProfanity(request.getTitle());
        if (pollTitle != null && !pollTitle.equals(poll.getTitle())) {
            poll.setTitle(pollTitle);
        }
        Boolean allowMultipleVotes = request.isAllowMultipleVotes();
        if (!allowMultipleVotes.equals(poll.isAllowedMultipleVotes())) {
            poll.setAllowMultipleVotes(allowMultipleVotes);
        }
        List<PostDto.PollOptionRequest> pollOptionsRequest = request.getPollOptions();
        updatePollOptions(pollOptionsRequest, poll);
        return new PostDto.PollDto(poll, List.of(), true);
    }


    @Override
    public boolean bookmarkPost(Long postId, Long userId) {
        boolean marked = true;
        PostBookmark bookmark = bookmarkRepository.findByPostingIdAndUserId(postId, userId)
                .orElse(null);
        if (bookmark == null) {
            Posting post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
            bookmark = new PostBookmark(post, userId);
            bookmarkRepository.save(bookmark);
        }
        else {
            marked = false;
            bookmarkRepository.delete(bookmark);
        }
        return marked;
    }

    @Override
    public Page<PostDto.ListItem> getBookmarkedPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Posting> postPages = bookmarkRepository.findPostsByUserId(userId, pageable);
        Set<Long> userIds = postPages.stream()
                .map(Posting::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Posting> posts = postPages.stream()
                .toList();
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts);
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, posts);
        List<Long> commentedPostIds =
                postRepository.getAllIdsInListCommentedByUserId(userId, posts);
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(posts);
        return postPages.map(post -> {
            User user = userMap.get(post.getUserId());
            return new PostDto.ListItem(post, user, tagMap.getOrDefault(post.getId(), List.of()),
                    likedPostIds.contains(post.getId()), viewedPostIds.contains(post.getId()),
                    commentedPostIds.contains(post.getId()));
        });
    }

    @Override
    public Page<PostDto.ListItem> getVotedPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Posting> postPages = voteRepository.findVotedPostsByUserId(userId, pageable);
        Set<Long> userIds = postPages.stream()
                .map(Posting::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Posting> posts = postPages.stream()
                .toList();
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, posts);
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId, posts);
        List<Long> commentedPostIds =
                postRepository.getAllIdsInListCommentedByUserId(userId, posts);
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(posts);
        return postPages.map(post -> {
            User user = userMap.get(post.getUserId());
            return new PostDto.ListItem(post, user, tagMap.getOrDefault(post.getId(), List.of()),
                    likedPostIds.contains(post.getId()), viewedPostIds.contains(post.getId()),
                    commentedPostIds.contains(post.getId()));
        });
    }

    @Override
    public Page<PostDto.RequestedItem> getFairViewConfirmList(Long userId, Pageable pageable) {
        List<User> users = userRepository.findPartnerAndUserById(userId);
        if (users.isEmpty()) {
            throw new BusinessException(ExceptionCode.USER_NOT_FOUND);
        }
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();
        Page<Posting> postPages =
                postRepository.findAllUnconfirmedPostsByUserIdIn(userIds, pageable);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Posting> postList = postPages.stream()
                .toList();
        List<Long> postIds = postList.stream()
                .map(Posting::getId)
                .toList();
        List<FairView> usersFairViews =
                fairViewRepository.findAllByUserIdAndPostIdIn(userId, postIds);
        Map<Long, FairView> fairViewMap = new HashMap<>();
        for (FairView fairView : usersFairViews) {
            fairViewMap.put(fairView.getPosting()
                    .getId(), fairView);
        }

        Map<Long, NotificationDto.ContentUpdateStatus> contentUpdates =
                contentUpdateService.getContentUpdatesByUserId(userId, postList, Posting::getId,
                        ItemType.POST);
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(postList);
        return postPages.map(post -> {
            User user = userMap.get(post.getUserId());
            return new PostDto.RequestedItem(post, user,
                    tagMap.getOrDefault(post.getId(), List.of()), contentUpdates.get(post.getId()),
                    fairViewMap.get(post.getId()));
        });
    }

    @Override
    public boolean confirmFairViewPost(Long userId, Long postId) {
        Posting post = postRepository.findByIdAndPostType(postId, PostingType.FAIR_VIEW)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (fairViewRepository.existsByPostingAndConfirmedIsFalse(post)) {
            throw new BusinessException(ExceptionCode.FAIR_VIEW_NOT_CONFIRMED);
        }
        post.setConfirmed(true);
        return true;
    }

    @Override
    public boolean confirmFairView(Long userId, Long fairViewId) {
        FairView fairView = fairViewRepository.findById(fairViewId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        fairView.setConfirmed(true);
        return true;
    }

    @Override
    public Page<PostDto.ListItem> searchPostsByKeyword(Long userId, String keyword,
            Pageable pageable) {

        List<Long> blockedPostIds = reportService.getBlockedTargetIds(userId, ItemType.POST);
        List<Long> blockedUserIds = reportService.getBlockedTargetIds(userId, ItemType.USER);


        String normalizedKeyword = normalizeKeyword(keyword);

        List<Posting> prioritizedResults =
                searchWithPriority(normalizedKeyword, blockedPostIds, blockedUserIds);

        if (prioritizedResults.isEmpty()) {
            return Page.empty(pageable);
        }

        // 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), prioritizedResults.size());

        if (start >= prioritizedResults.size()) {
            return Page.empty(pageable);
        }

        List<Posting> pagedResults = prioritizedResults.subList(start, end);

        // DTO 변환
        List<PostDto.ListItem> searchResults = convertToListItems(pagedResults, userId);

        return new PageImpl<>(searchResults, pageable, prioritizedResults.size());
    }

    // 우선순위 기반 검색 메서드
    private List<Posting> searchWithPriority(String keyword, List<Long> blockedPostIds,
            List<Long> blockedUserIds) {
        Set<Long> addedIds = new HashSet<>();
        List<Posting> orderedResults = new ArrayList<>();

        // 1. 제목 정확 일치 (최고 우선순위)
        List<Posting> titleExact =
                postRepository.searchByTitleExact(keyword, blockedPostIds, blockedUserIds);
        addUniqueResults(orderedResults, addedIds, titleExact);

        // 2. 제목 부분 일치 (높은 우선순위)
        List<Posting> titlePartial =
                postRepository.searchByTitlePartial(keyword, blockedPostIds, blockedUserIds);
        addUniqueResults(orderedResults, addedIds, titlePartial);

        // 3. Poll 제목 검색
        List<Posting> pollTitle =
                postRepository.searchByPollTitle(keyword, blockedPostIds, blockedUserIds);
        addUniqueResults(orderedResults, addedIds, pollTitle);

        // 4. 내용 검색 (중간 우선순위)
        List<Posting> content =
                postRepository.searchByContent(keyword, blockedPostIds, blockedUserIds);
        addUniqueResults(orderedResults, addedIds, content);

        // 5. FairView 내용 검색
        List<Posting> fairViewContent =
                postRepository.searchByFairViewContent(keyword, blockedPostIds, blockedUserIds);
        addUniqueResults(orderedResults, addedIds, fairViewContent);

        // 6. 태그 검색 (낮은 우선순위)
        List<Posting> tags = postRepository.searchByTags(keyword, blockedPostIds, blockedUserIds);
        addUniqueResults(orderedResults, addedIds, tags);

        return orderedResults;
    }

    // 중복 제거 헬퍼 메서드
    private void addUniqueResults(List<Posting> orderedResults, Set<Long> addedIds,
            List<Posting> newResults) {
        for (Posting post : newResults) {
            if (!addedIds.contains(post.getId())) {
                addedIds.add(post.getId());
                orderedResults.add(post);
            }
        }
    }

    // 키워드 정규화 메서드
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim()
                .isEmpty()) {
            return "";
        }
        return keyword.trim()
                .replaceAll("\\s+", " ");
    }

    // DTO 변환 메서드
    private List<PostDto.ListItem> convertToListItems(List<Posting> posts, Long userId) {
        if (posts.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = posts.stream()
                .map(Posting::getUserId)
                .collect(Collectors.toSet());

        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Long> likedPostIds = userId == null ? List.of() :
                postRepository.getAllIdsInListLikedByUserId(userId, posts);
        List<Long> viewedPostIds = userId == null ? List.of() :
                postRepository.getAllIdsInListViewedByUserId(userId, posts);
        List<Long> commentedPostIds = userId == null ? List.of() :
                postRepository.getAllIdsInListCommentedByUserId(userId, posts);

        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(posts);

        List<PostDto.ListItem> response = new ArrayList<>();
        for (Posting post : posts) {
            User user = userMap.get(post.getUserId());
            response.add(
                    new PostDto.ListItem(post, user, tagMap.getOrDefault(post.getId(), List.of()),
                            likedPostIds.contains(post.getId()),
                            viewedPostIds.contains(post.getId()),
                            commentedPostIds.contains(post.getId())));
        }

        return response;
    }

    // 5. 추가 개선사항 - 다중 키워드 검색 지원

    // 공백으로 구분된 여러 키워드 검색
    private List<Posting> searchMultipleKeywords(String keyword, List<Long> blockedPostIds,
            List<Long> blockedUserIds) {
        String[] keywords = keyword.split("\\s+");

        if (keywords.length == 1) {
            return searchWithPriority(keyword, blockedPostIds, blockedUserIds);
        }

        // 모든 키워드가 포함된 게시물 검색 (AND 조건)
        Set<Long> intersectionIds = null;

        for (String singleKeyword : keywords) {
            List<Posting> singleResults =
                    searchWithPriority(singleKeyword.trim(), blockedPostIds, blockedUserIds);
            Set<Long> currentIds = singleResults.stream()
                    .map(Posting::getId)
                    .collect(Collectors.toSet());

            if (intersectionIds == null) {
                intersectionIds = currentIds;
            }
            else {
                intersectionIds.retainAll(currentIds);
            }

            if (intersectionIds.isEmpty()) {
                break;
            }
        }

        if (intersectionIds == null || intersectionIds.isEmpty()) {
            // AND 조건으로 찾지 못한 경우, OR 조건으로 다시 검색
            return searchWithPriority(keyword, blockedPostIds, blockedUserIds);
        }

        // 교집합 결과를 다시 우선순위 순으로 정렬
        return postRepository.findAllById(intersectionIds)
                .stream()
                .sorted((p1, p2) -> compareBySearchPriority(p1, p2, keyword))
                .collect(Collectors.toList());
    }

    // 검색 우선순위 비교 메서드
    private int compareBySearchPriority(Posting p1, Posting p2, String keyword) {
        // 제목 정확 일치가 최우선
        boolean p1TitleExact = p1.getTitle()
                .toLowerCase()
                .equals(keyword.toLowerCase());
        boolean p2TitleExact = p2.getTitle()
                .toLowerCase()
                .equals(keyword.toLowerCase());

        if (p1TitleExact && !p2TitleExact) return -1;
        if (!p1TitleExact && p2TitleExact) return 1;

        // 제목 포함 여부
        boolean p1TitleContains = p1.getTitle()
                .toLowerCase()
                .contains(keyword.toLowerCase());
        boolean p2TitleContains = p2.getTitle()
                .toLowerCase()
                .contains(keyword.toLowerCase());

        if (p1TitleContains && !p2TitleContains) return -1;
        if (!p1TitleContains && p2TitleContains) return 1;

        // 생성일 기준 내림차순 (최신순)
        return p2.getCreatedAt()
                .compareTo(p1.getCreatedAt());
    }

    @Override
    public void setNickname(Long userId, String nickname) {
        List<Posting> posts = postRepository.findAllByUserId(userId);

    }

    @CacheEvict(value = "topPosts", allEntries = true)
    @Override
    public void evictAllTopPosts() {
        log.debug("Evicting all cached top posts");
        // This method will clear the cache for all top posts
    }

    @Override
    public boolean reportPost(Long userId, Long postId, ReportDto.Request reportRequest) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        int reportedCount = reportService.report(userId, postId, ItemType.POST, reportRequest);
        if (reportedCount >= 10) {
            post.setReported(true);
        }
        evictTopPostsCache(userId);
        return true;
    }

    @Override
    public Page<PostDto.RequestedListItem> getFairViewRequestedList(Long userId, int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id")
                .descending());
        Page<Posting> postPages = fairViewRepository.findPostsByUserId(userId, pageable);
        Set<Long> userIds = postPages.stream()
                .map(Posting::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Posting> posts = postPages.stream()
                .toList();
        Map<Long, NotificationDto.ContentUpdateStatus> contentUpdateStatusMap =
                contentUpdateService.getContentUpdatesByUserId(userId, posts, Posting::getId,
                        ItemType.POST);
        return postPages.isEmpty() ? Page.empty(pageable) : postPages.map(post -> {
            User user = userMap.get(post.getUserId());
            NotificationDto.ContentUpdateStatus contentUpdateStatus =
                    contentUpdateStatusMap.getOrDefault(post.getId(), null);

            Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(posts);
            boolean requested = contentUpdateStatus != null && contentUpdateStatus.isUnread();
            return new PostDto.RequestedListItem(post, user,
                    tagMap.getOrDefault(post.getId(), List.of()), requested);
        });
    }

    @Override
    public List<PostDto.ListItem> getAllReportedPosts() {
        List<Posting> posts = postRepository.findAllByReportedTrue();
        if (posts.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = posts.stream()
                .map(Posting::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(null, posts);
        List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(null, posts);
        List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(null, posts);
        List<Posting> postList = posts.stream()
                .toList();
        Map<Long, List<Tag>> tagMap = tagService.getPostTagsMap(postList);
        List<PostDto.ListItem> response = new ArrayList<>();
        for (Posting post : posts) {
            User user = userMap.get(post.getUserId());
            response.add(
                    new PostDto.ListItem(post, user, tagMap.getOrDefault(post.getId(), List.of()),
                            likedPostIds.contains(post.getId()),
                            viewedPostIds.contains(post.getId()),
                            commentedPostIds.contains(post.getId())));
        }
        return response;

    }

    @Override
    public boolean unblockPost(Long postId) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        post.setReported(false);
        return true;
    }

    @Override
    public boolean deletePostByAdmin(Long postId) {
        Posting post = postRepository.findByIdAndReportedIsTrue(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        postRepository.delete(post);
        PostDto.Notification info = new PostDto.Notification(post);
        notificationService.notifyPostDeleted(post.getUserId(), info);
        return true;
    }

    @Override
    public Page<PostDto.HotFairView> getFairViewList(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt")
                .descending());

        List<Long> blockedPostIds = reportService.getBlockedTargetIds(userId, ItemType.POST);
        List<Long> blockedUserIds = reportService.getBlockedTargetIds(userId, ItemType.USER);
        Page<Posting> posts =
                postRepository.findAllByPostType(PostingType.FAIR_VIEW, blockedPostIds,
                        blockedUserIds, pageable);
        if (posts.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Posting> postList = posts.stream()
                .toList();

        List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId, postList);
        List<FairView> fairViews = fairViewRepository.findAllByPostingIdIn(postList.stream()
                .map(Posting::getId)
                .toList());
        Set<Long> userIds = fairViews.stream()
                .map(FairView::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, List<FairView>> fairViewMap = new HashMap<>();
        for (FairView fairView : fairViews) {
            fairViewMap.computeIfAbsent(fairView.getPosting()
                            .getId(), k -> new ArrayList<>())
                    .add(fairView);
        }

        return posts.map(post -> {
            List<PostDto.FairViewItem> fairViewItems =
                    fairViewMap.getOrDefault(post.getId(), List.of())
                            .stream()
                            .map(fv -> new PostDto.FairViewItem(fv, userMap.get(fv.getUserId())))
                            .toList();
            return new PostDto.HotFairView(post, fairViewItems,
                    likedPostIds.contains(post.getId()));
        });
    }

    @Override
    public PostDto.ListItem createVirtualPost(AdminDto.VirtualPostRequest request) {
        Long userId = request.getUserId();
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        PostingType postType = PostingType.valueOf(request.getPostType()
                .toUpperCase());
        Posting post = Posting.builder()
                .title(request.getTitle())
                .postType(postType)
                .content(request.getContent())
                .userId(userId)
                .build();
        postRepository.save(post);
        if (postType == PostingType.POLL) {
            Poll poll = new Poll(request.getPollTitle(), post, request.isAllowMultipleVotes());
            pollRepository.save(poll);

            List<PostDto.PollOptionRequest> pollOptionsRequest = request.getPoll()
                    .getPollOptions();
            List<PollOption> pollOptions = new ArrayList<>();
            for (int i = 0; i < pollOptionsRequest.size(); i++) {
                PostDto.PollOptionRequest pollOptionRequest = pollOptionsRequest.get(i);
                PollOption pollOption = PollOption.builder()
                        .name(pollOptionRequest.getName())
                        .poll(poll)
                        .index(i)
                        .build();
                pollOptions.add(pollOption);
            }
            pollOptionRepository.saveAll(pollOptions);
            poll.setPollOptions(pollOptions);
            post.setPoll(poll);
        }
        else if (postType == PostingType.FAIR_VIEW) {
            Long partnerId = user.getPartnerId();
            if (partnerId == null) {
                throw new BusinessException(ExceptionCode.PARTNER_NOT_FOUND);
            }
            User partner = userRepository.findPartnerById(partnerId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.PARTNER_NOT_FOUND));
            PostDto.FairViewItem fairView = request.getFairViewItem();
            List<FairView> fairViews = new ArrayList<>();
            FairView myFairView = FairView.builder()
                    .title(fairView.getTitle())
                    .content(fairView.getContent())
                    .post(post)
                    .userId(userId)
                    .nickname(user.getNickname())
                    .build();
            myFairView.setConfirmed(true);
            FairView partnerFairView = new FairView(partner, post);
            notificationService.sendFairViewRequest(post.getId(), partner);
            contentUpdateService.fairViewRequestUpdate(post.getId(), partner.getId());

            fairViews.add(partnerFairView);
            fairViews.add(myFairView);
            fairViewRepository.saveAll(fairViews);
            post.addFairView(myFairView);
            post.addFairView(partnerFairView);
        }
        Set<Tag> tags = tagService.createTags(request.getTags());
        List<TagMapping> tagResponses = tagService.createTagMapping(tags, post);
        post.setTags(tagResponses);
        return new PostDto.ListItem(post, user, tagResponses.stream()
                .map(TagMapping::getTag)
                .toList(), false, false, false);
    }

    @Override
    public PostDto.ListItem updateVirtualPost(Long postId, PostDto.Request request) {
        Posting post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
        if (!userRepository.isVirtualUser(post.getUserId())) {
            throw new BusinessException(ExceptionCode.FORBIDDEN);
        }
        post.update(request);
        if (post.getPostType() == PostingType.POLL && request.getPollId() != null) {
            Long pollId = request.getPollId();
            PostDto.PollRequest pollDto = request.getPoll();
            String pollTitle = pollDto.getTitle();
            Boolean allowMultipleVotes = pollDto.isAllowMultipleVotes();
            Poll poll = pollRepository.findById(pollId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.POLL_NOT_FOUND));
            if (pollTitle != null && !pollTitle.equals(poll.getTitle())) {
                poll.setTitle(pollTitle);
            }
            if (!allowMultipleVotes.equals(poll.isAllowedMultipleVotes())) {
                poll.setAllowMultipleVotes(allowMultipleVotes);
            }
            List<PostDto.PollOptionRequest> pollOptionsRequest = pollDto.getPollOptions();
            updatePollOptions(pollOptionsRequest, poll);
        }
        Set<Tag> tags = tagService.createTags(request.getTags());
        List<TagMapping> tagResponses = tagService.updateTags(tags, post);
        post.getTags()
                .clear();
        post.getTags()
                .addAll(tagResponses);
        postRepository.save(post);
        return new PostDto.ListItem(post, userRepository.findById(post.getUserId())
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND)),
                tagResponses.stream()
                        .map(TagMapping::getTag)
                        .toList(), false, false, false);
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

    @Override
    public PostDto.ListItem createFairViewByAdmin(AdminDto.FairViewPostRequest request) {

        PostingType postType = request.getPostType();
        if (postType != PostingType.FAIR_VIEW) throw new BusinessException(ExceptionCode.FORBIDDEN);
        Long userId = request.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        Posting post = Posting.builder()
                .title(request.getTitle())
                .postType(postType)
                .content(request.getContent())
                .userId(userId)
                .build();
        post.setConfirmed(true);
        postRepository.save(post);

        List<FairView> fairViews = new ArrayList<>();
        for (AdminDto.FairViewRequest fairViewRequest : request.getFairViewItem()) {
            FairView fairView = FairView.builder()
                    .title(fairViewRequest.getTitle())
                    .content(fairViewRequest.getContent())
                    .post(post)
                    .userId(fairViewRequest.getUserId())
                    .build();
            fairView.setConfirmed(true);
            fairViews.add(fairView);
        }
        fairViewRepository.saveAll(fairViews);
        post.setFairViews(fairViews);
        Set<Tag> tags = tagService.createTags(request.getTags());
        List<TagMapping> tagResponses = tagService.createTagMapping(tags, post);
        post.setTags(tagResponses);
        List<Tag> tagList = tags.stream()
                .toList();
        return new PostDto.ListItem(post, user, tagList, false, false, false);
    }
}
