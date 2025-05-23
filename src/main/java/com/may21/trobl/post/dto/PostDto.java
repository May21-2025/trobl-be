package com.may21.trobl.post.dto;

import static com.may21.trobl._global.utility.SecurityUtils.decodeHtml;
import static com.may21.trobl._global.utility.SecurityUtils.escapeHtml;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.post.domain.PairView;
import com.may21.trobl.post.domain.Poll;
import com.may21.trobl.post.domain.PollOption;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.user.domain.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PostDto {

  @Getter
  @NoArgsConstructor
  public abstract static class BasePostDto {
    protected Long postId;
    protected String username;
    protected int viewCount;

    protected BasePostDto(Posting post, User user) {
      this.postId = post.getId();
      this.username = user != null ? user.getNickname() : post.getNickname();
      this.viewCount = post.getViewCount();
    }
  }

  @Getter
  @NoArgsConstructor
  public abstract static class BasicPostWithTitle extends BasePostDto {
    protected String title;

    protected BasicPostWithTitle(Posting post, User user) {
      super(post, user);
      this.title = decodeHtml(post.getTitle());
    }
  }




  @Getter
  @NoArgsConstructor
  public static class Card extends BasicPostWithTitle {
    private int commentCount;

    public Card(Posting post) {
      super(post, null); // username은 Posting 기반
      this.commentCount = post.getComments().size();
    }
  }

  @Getter
  @NoArgsConstructor
  public static class ListItem extends BasicPostWithTitle {
    private String content;
    private LocalDateTime createdAt;
    private int commentCount;
    private int likeCount;
    private boolean liked;
    private boolean viewed;
    private boolean commented;
    private PostingType postType;

    public ListItem(Posting post, User user, Boolean liked, Boolean viewed, Boolean commented) {
      super(post, user);
      this.createdAt = post.getCreatedAt();
      this.commentCount = post.getComments().size();
      this.likeCount = post.getPostLikes().size();
      this.liked = Boolean.TRUE.equals(liked);
      this.viewed = Boolean.TRUE.equals(viewed);
      this.commented = Boolean.TRUE.equals(commented);
        this.postType = post.getPostType();

      String raw = decodeHtml(post.getContent());
      this.content = raw==null? "" : raw.length() > 100 ? raw.substring(0, 100) + "..." : raw;
    }
  }

  @Getter
  @NoArgsConstructor
  public static class QuickPoll extends BasePostDto {
    private Long userId;
    private LocalDateTime createdAt;
    private PollDto pollDto;

    public QuickPoll(Posting post) {
      super(post, null);
      this.userId = post.getUserId();
      this.createdAt = post.getCreatedAt();
      this.pollDto = new PollDto(post.getPoll());
    }
  }
  @Getter
  @NoArgsConstructor
  public static class Detail extends BasicPostWithTitle {
    private Long userId;
    private LocalDateTime createdAt;
    private PollDto pollDto;
    private List<OpinionItem> opinions;
    private String content;
    private int shareCount;
    private boolean liked;
    private boolean bookmarked;
    private String postType;

    public Detail(Posting post, User user, Map<Long, User> userMap, boolean liked, boolean bookmarked) {
      super(post, user);
      this.userId = post.getUserId();
      this.createdAt = post.getCreatedAt();
      this.pollDto = post.getPoll() != null ? new PollDto(post.getPoll()) : null;
      this.opinions = OpinionItem.fromPairViews(post.getPairViews(), userMap);
      this.shareCount = post.getShareCount();
      this.postType = post.getPostType().name();
      this.content = decodeHtml(post.getContent());
      this.liked = liked;
      this.bookmarked = bookmarked;
    }
  }

  @Getter
  @NoArgsConstructor
  public static class PollDto {
    private Long pollId;
    private String title;
    private List<PollItem> pollOptions;

    public PollDto(Poll poll) {
        this.pollId = poll.getId();
      this.title = poll.getTitle();
      this.pollOptions = PollItem.fromPollOption(poll.getPollOptions());
    }
  }

  @Getter
  @NoArgsConstructor
  public static class OpinionItem {
    private String title;
    private String nickname;
    private String content;

    public OpinionItem(PairView pairView, User user) {
      String nickname = user==null ? pairView.getNickname() : user.getNickname();
      this.content = decodeHtml(pairView.getContent());
      this.title = decodeHtml(pairView.getTitle());
      this.nickname = nickname;
    }

    public static List<OpinionItem> fromPairViews(
        List<PairView> pairViews, Map<Long, User> userMap) {
      if (pairViews == null) {
        return new ArrayList<>();
      }
      List<OpinionItem> opinionItems = new ArrayList<>();
      for (PairView pairView : pairViews) {
        opinionItems.add(new OpinionItem(pairView, userMap.get(pairView.getUserId())));
      }
      return opinionItems;
    }
  }

  @Getter
  @NoArgsConstructor
  public static class Request {
    private String title;
    private String content;
    private String postType;
    private OpinionItem optionItem;
    private PollDto poll;

    public String getTitle() {
      return escapeHtml(title);
    }

    public String getPollTitle() {
      return poll ==null? "" : escapeHtml(poll.title);
    }
    public Long getPollId() {
      return poll ==null? null : poll.pollId;
    }


    public String getContent() {
      return escapeHtml(content);
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class PollItem {
    private Long pollOptionId;
    private String name;
    private String content;
    private long voteCount = 0;
    private int index = 0;
    private boolean voted = false;

    public PollItem(PollOption polloption) {
      this.pollOptionId = polloption.getId();
      this.name = decodeHtml(polloption.getName());
      this.content = decodeHtml(polloption.getContent());
      this.voteCount = polloption.getVoteCount();
      this.index = polloption.getIndex();
    }

    public static List<PollItem> fromPollOption(List<PollOption> pollOptions) {
      List<PollItem> pollList = new ArrayList<>();
      for (PollOption polloption : pollOptions) {
        pollList.add(new PollItem(polloption));
      }
      return pollList;
    }
  }
}
