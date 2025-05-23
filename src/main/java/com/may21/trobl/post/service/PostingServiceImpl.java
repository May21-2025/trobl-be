package com.may21.trobl.post.service;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.post.domain.*;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostingServiceImpl implements PostingService {
  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final PollOptionRepository pollOptionRepository;
  private final PairViewRepository pairViewRepository;
  private final VoteRepository voteRepository;
  private final PostLikeRepository likeRepository;
  private final PostBookmarkRepository bookmarkRepository;
  private final PostViewRepository viewRepository;
  private final PollRepository pollRepository;

  @Override
  public Page<PostDto.ListItem> getPostsList(Pageable pageable, Long userId) {
    Page<Posting> posts = postRepository.findAll(pageable);
    Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(userIds);
    Map<Long, User> userMap =
        users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId);
    List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId);
    List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId);
    return posts.map(
        post -> {
          Long postId = post.getId();
          User owner = userMap.get(post.getUserId());
          return new PostDto.ListItem(post, owner, likedPostIds.contains(postId), viewedPostIds.contains(postId), commentedPostIds.contains(postId));
        });
  }

  @Override
  public List<PostDto.Card> getTop10Views(String type) {

    LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);

    List<Posting> posts =
        switch (type.toLowerCase()) {
          case "like" -> postRepository.findTopPostsByLikes(10, PostingType.POLL);
          case "view" -> postRepository.findTopPostsByViews(10, PostingType.POLL);
          case "share" -> postRepository.findTopPostsByShares(10, PostingType.POLL);
          case "comment" -> postRepository.findTopPostsByComments(10, PostingType.POLL);
          case "vote" -> postRepository.findTopPostsByVotes(10);
          default -> postRepository.findTopPostsByLikesAndViews(10, threeMonthsAgo, PostingType.POLL);
        };

    List<PostDto.Card> response = new ArrayList<>();
    for (Posting post : posts) {
      response.add(new PostDto.Card(post));
    }
    return response;
  }

  @Override
  public PostDto.Detail getPostDetail(Long postId, User user) {
    Posting post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
    post.incrementViewCount();
    User owner =
        userRepository
            .findById(post.getUserId())
            .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
    Map<Long, User> userMap = new HashMap<>();
    boolean liked = false;
    boolean bookmarked = false;
    if (user != null) {
        liked = likeRepository.existsByPostingIdAndUserId(postId, user.getId());
        bookmarked = bookmarkRepository.existsByPostingIdAndUserId(postId, user.getId());
        if(viewRepository.existsByPostIdAndUserId(postId, user.getId())) {
            post.incrementViewCount();
        } else {
            viewRepository.save(new PostView(post, user.getId()));
        }
    }
    return new PostDto.Detail(post, owner, userMap, liked, bookmarked);
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
            .pollTitle(request.getPollTitle())
            .nickname(user.getNickname())
            .build();
    postRepository.save(post);
    if(postType==PostingType.POLL) {
      Poll poll = new Poll(request.getPollTitle(), post);
      pollRepository.save(poll);

      List<PostDto.PollItem > pollOptionsRequest = request.getPoll().getPollOptions();
      List<PollOption> pollOptions = new ArrayList<>();
      for (int i = 0; i < pollOptionsRequest.size(); i++) {
        PostDto.PollItem pollOptionRequest = pollOptionsRequest.get(i);
        PollOption pollOption =
                PollOption.builder()
                        .name(pollOptionRequest.getName())
                        .poll(poll)
                        .content(pollOptionRequest.getContent())
                        .index(i)
                        .build();
        pollOptions.add(pollOption);
      }
      pollOptionRepository.saveAll(pollOptions);
      poll.setPollOptions(pollOptions);
    } else if (postType ==PostingType.PAIR_VIEW) {
      PostDto.OpinionItem opinionItem = request.getOptionItem();
      PairView pairView = PairView.builder()
          .title(opinionItem.getTitle())
          .content(opinionItem.getContent())
          .post(post)
          .userId(userId)
          .build();
      pairViewRepository.save(pairView);
      post.setPairViews(List.of(pairView));
    }
    Map<Long, User> userMap = new HashMap<>();
    userMap.put(userId, user);
    return new PostDto.Detail(post, user, userMap, false, false);
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
    post.setTitle(request.getTitle());
    post.setContent(request.getContent());
    if(post.getPostType() != PostingType.POLL && request.getPollId() != null) {
      Long pollId = request.getPollId();
      Poll poll = pollRepository.findById(pollId).orElseThrow(() -> new BusinessException(ExceptionCode.POLL_NOT_FOUND));
      poll.setTitle(request.getPollTitle());
      List<PostDto.PollItem> pollOptionsRequest = request.getPoll().getPollOptions();
      updatePollOptions(pollOptionsRequest, poll);
    }
    postRepository.save(post);

    Map<Long, User> userMap = new HashMap<>();
    return new PostDto.Detail(
            post,
            userRepository
                    .findById(userId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND)),
            userMap, false, false);
  }

  private List<PollOption> updatePollOptions(
      List<PostDto.PollItem> pollOptionsRequest, Poll poll) {
    List<PollOption> pollOptions = poll.getPollOptions();
    List<PollOption> newPollOptions = new ArrayList<>();
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
                .content(pollOptionRequest.getContent())
                .index(i)
                .poll(poll)
                .build();
        newPollOptions.add(pollOption);
      } else {
        // Update existing poll option
        PollOption existingOption =
            pollOptions.stream()
                .filter(option -> option.getId().equals(pollOptionRequest.getPollOptionId()))
                .findFirst()
                .orElse(
                    new PollOption(
                        pollOptionRequest.getName(), pollOptionRequest.getContent(), i, poll));
        existingOption.setName(pollOptionRequest.getName());
        existingOption.setContent(pollOptionRequest.getContent());
        existingOption.setIndex(i);
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
    if (!newPollOptions.isEmpty()) {
      pollOptions.addAll(newPollOptions);
      pollOptionRepository.saveAll(newPollOptions);
    }
    poll.setPollOptions(pollOptions);
    return pollOptions;
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
  public boolean likePost(Long postId, Long userId) {
    boolean liked = true;
    PostLike postLike = likeRepository.findByPostingIdAndUserId(postId, userId).orElse(null);
    if (postLike == null) {
      Posting post =
          postRepository
              .findById(postId)
              .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
      postLike = new PostLike(post, userId);
      likeRepository.save(postLike);
    } else {
      liked = false;
      likeRepository.deleteByEntity(postLike);
    }
    return liked;
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
    Page<Posting> posts = postRepository.findByUserId(userId,PageRequest.of(page, size));
    Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(userIds);
    Map<Long, User> userMap =
        users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId);
    List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId);
    List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId);
    List<PostDto.ListItem> response = new ArrayList<>();
    for (Posting post : posts) {
      response.add(new PostDto.ListItem(post, userMap.get(post.getUserId()), likedPostIds.contains(post.getId()), viewedPostIds.contains(post.getId()), commentedPostIds.contains(post.getId())));
    }
    return new PageImpl<>(response, PageRequest.of(page, size), posts.getTotalElements());
  }

  @Override
  public Page<PostDto.ListItem> getLikedPosts(Long userId, int page, int size) {
    Page<Posting> posts = likeRepository.findPostingByUserId(userId,PageRequest.of(page, size));
    Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(userIds);
    Map<Long, User> userMap =
        users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    List<Long> viewedPostIds = postRepository.getAllIdsInListViewedByUserId(userId);
    List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId);
    return posts.map(
            post -> {
              User user = userMap.get(post.getUserId());
              return new PostDto.ListItem(post, user, true, viewedPostIds.contains(post.getId()), commentedPostIds.contains(post.getId()));
            });
  }

  @Override
  public Page<PostDto.ListItem> getVisitedPosts(Long userId, int page, int size) {
    Page<Posting> posts = viewRepository.findPostingByUserId(userId,PageRequest.of(page, size));
    Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(userIds);
    Map<Long, User> userMap =
        users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    List<Long> likedPostIds = postRepository.getAllIdsInListLikedByUserId(userId);
    List<Long> commentedPostIds = postRepository.getAllIdsInListCommentedByUserId(userId);
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
                        .orElseThrow(() -> new BusinessException(ExceptionCode.PAIR_VIEW_CAN_NOT_BE_ADDED));
      User user =
              userRepository
                      .findById(userId)
                      .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        PairView pairView = PairView.builder()
                .title(opinionItem.getTitle())
                .content(opinionItem.getContent())
                .post(post)
                .userId(user.getId())
                .nickname(user.getNickname())
                .build();
        pairViewRepository.save(pairView);
        Map<Long, User> userMap = new HashMap<>();
        userMap.put(userId, user);
        post.addPairView(pairView);
        return new PostDto.Detail(post, user, userMap, false, false);
    }

    @Override
    public List<PostDto.QuickPoll> getRandomQuickPoll() {
        List<Posting> posts = postRepository.findRandomPostsByType(5,PostingType.POLL);
        List<PostDto.QuickPoll> response = new ArrayList<>();
        for (Posting post : posts) {
            response.add(new PostDto.QuickPoll(post));
        }
        return response;
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
}
