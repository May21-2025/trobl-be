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

  @Override
  public Page<PostDto.ListItem> getPostsList(Pageable pageable) {
    Page<Posting> posts = postRepository.findAll(pageable);
    Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(userIds);
    Map<Long, User> userMap =
        users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    return posts.map(
        post -> {
          User user = userMap.get(post.getUserId());
          return new PostDto.ListItem(post, user);
        });
  }

  @Override
  public List<PostDto.View> getTop5Views() {
    LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
    List<Posting> posts = postRepository.findTopPostsByLikesAndViews(5, threeMonthsAgo, PostingType.POLL);
    Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(userIds);
    Map<Long, User> userMap =
        users.stream().collect(Collectors.toMap(User::getId, Function.identity()));

    List<PostDto.View> response = new ArrayList<>();
    for (Posting post : posts) {
      User user = userMap.get(post.getUserId());
      response.add(new PostDto.View(post, user, userMap));
    }
    return response;
  }

  @Override
  public List<PostDto.ListItem> getTop10Views(String type) {

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

    List<PostDto.ListItem> response = new ArrayList<>();
    for (Posting post : posts) {
      response.add(new PostDto.ListItem(post));
    }
    return response;
  }

  @Override
  public PostDto.Detail getPostDetail(Long postId) {
    Posting post =
        postRepository
            .findById(postId)
            .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
    post.incrementViewCount();
    User user =
        userRepository
            .findById(post.getUserId())
            .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
    Map<Long, User> userMap = new HashMap<>();
    return new PostDto.Detail(post, user, userMap);
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
      List<PostDto.PollItem > pollOptionsRequest = request.getPoll().getPollOptions();
      List<PollOption> pollOptions = new ArrayList<>();
      for (int i = 0; i < pollOptionsRequest.size(); i++) {
        PostDto.PollItem pollOptionRequest = pollOptionsRequest.get(i);
        PollOption pollOption =
                PollOption.builder()
                        .name(pollOptionRequest.getName())
                        .content(pollOptionRequest.getContent())
                        .index(i)
                        .post(post)
                        .build();
        pollOptions.add(pollOption);
      }
      pollOptionRepository.saveAll(pollOptions);
      post.setPollOptions(pollOptions);
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
    return new PostDto.Detail(post, user, userMap);
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
    post.setPollTitle(request.getPollTitle());
    List<PostDto.PollItem> pollOptionsRequest = request.getPoll().getPollOptions();
    post.setPollOptions(updatePollOptions(pollOptionsRequest, post));
    postRepository.save(post);

    Map<Long, User> userMap = new HashMap<>();
    return new PostDto.Detail(
        post,
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND)),
        userMap);
  }

  private List<PollOption> updatePollOptions(
      List<PostDto.PollItem> pollOptionsRequest, Posting post) {
    List<PollOption> pollOptions = post.getPollOptions();
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
                .post(post)
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
                        pollOptionRequest.getName(), pollOptionRequest.getContent(), i, post));
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
    List<PostDto.ListItem> response = new ArrayList<>();
    for (Posting post : posts) {
      response.add(new PostDto.ListItem(post));
    }
    return new PageImpl<>(response, PageRequest.of(page, size), posts.getTotalElements());
  }

  @Override
  public Page<PostDto.ListItem> getLikedPosts(Long id, int page, int size) {
    Page<Posting> posts = likeRepository.findPostingByUserId(id,PageRequest.of(page, size));
    Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
    List<User> users = userRepository.findAllById(userIds);
    Map<Long, User> userMap =
        users.stream().collect(Collectors.toMap(User::getId, Function.identity()));
    return posts.map(
            post -> {
              User user = userMap.get(post.getUserId());
              return new PostDto.ListItem(post, user);
            });
  }

  @Override
  public Page<PostDto.ListItem> getVisitedPosts(Long id, int page, int size) {
    throw new BusinessException(ExceptionCode.NOT_IMPLEMENTED);
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
      post.setPairViews(List.of(pairView));
        return new PostDto.Detail(post,user, userMap);
    }

    @Override
    public List<PostDto.View> getRandomQuickPoll() {
        List<Posting> posts = postRepository.findRandomPostsByType(5,PostingType.POLL);
        Set<Long> userIds = posts.stream().map(Posting::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap =
                users.stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<PostDto.View> response = new ArrayList<>();
        for (Posting post : posts) {
            User user = userMap.get(post.getUserId());
            response.add(new PostDto.View(post, user, userMap));
        }
        return response;
    }
}
