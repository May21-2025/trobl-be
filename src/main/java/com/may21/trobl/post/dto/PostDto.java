package com.may21.trobl.post.dto;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.post.domain.FairView;
import com.may21.trobl.post.domain.Poll;
import com.may21.trobl.post.domain.PollOption;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.user.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.may21.trobl._global.utility.SecurityUtils.decodeHtml;
import static com.may21.trobl._global.utility.SecurityUtils.escapeHtml;

public class PostDto {

  @Getter
  @NoArgsConstructor
  public abstract static class BasePostDto {
    protected Long postId;
    protected String username;
    protected int viewCount;

    protected BasePostDto(Posting post, User user) {
      this.postId = post.getId();
      this.username = post.getNickname();
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
    private int likeCount;

    public Card(Posting post, int commentCount) {
      super(post, null); // username은 Posting 기반
      this.commentCount = commentCount;
      likeCount = post.getLikeCount();
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
      this.commentCount = post.getCommentCount();
      this.likeCount = post.getLikeCount();
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
    private int voteCount;
    private PollDto poll;

    public QuickPoll(Posting post, List<Long> votedOptionIds, boolean isOwner) {
      super(post, null);
      Poll poll = post.getPoll();
      this.userId = post.getUserId();
      this.createdAt = post.getCreatedAt();
      PollDto pollDto = new PollDto(poll, votedOptionIds, isOwner);
      this.poll = pollDto;
      int voteCount = 0;
      for(PollItem pollItem : pollDto.getPollOptions()) {
        voteCount += pollItem.getVoteCount();
      }
      this.voteCount = voteCount;
    }
  }
  @Getter
  @NoArgsConstructor
  public static class Detail extends BasicPostWithTitle {
    private Long userId;
    private LocalDateTime createdAt;
    private PollDto poll;
    private List<OpinionItem> opinions;
    private String content;
    private int shareCount;
    private boolean liked;
    private boolean bookmarked;
    private String postType;
    private List<TagDto.Response> tags;
    private int commentCount;
    private int likeCount;

    public Detail(Posting post, User user, Map<Long, User> userMap, List<Tag> tags, boolean liked, boolean bookmarked, List<Long> postIds, boolean isOwner) {
      super(post, user);
      this.userId = post.getUserId();
      this.createdAt = post.getCreatedAt();
      this.poll = post.getPoll() != null ? new PollDto(post.getPoll(),postIds,isOwner) : null;
      this.opinions = OpinionItem.fromPairViews(post.getFairViews(), userMap);
      this.shareCount = post.getShareCount();
      this.postType = post.getPostType().name();
      this.content = decodeHtml(post.getContent());
      this.liked = liked;
      this.bookmarked = bookmarked;
        this.tags = TagDto.Response.fromTagList(tags);
        this.commentCount = post.getComments().size();
        this.likeCount = post.getPostLikes().size();
    }
  }

  @Getter
  @NoArgsConstructor
  public static class PollDto {
    private Long pollId;
    private String title;
    private boolean showPollResult;
    private List<PollItem> pollOptions;

    public PollDto(Poll poll, List<Long> votedOptionIds, boolean isOwner) {
      this.pollId = poll.getId();
      this.title = poll.getTitle();

      this.pollOptions = PollItem.fromPollOption(poll.getPollOptions(),votedOptionIds);

      boolean hasVoted = false;
      for (PollItem pollItem : pollOptions) {
      if(pollItem.isVoted()) {
        hasVoted = true;
        break;
      }

      }
        this.showPollResult = isOwner || hasVoted;
    }
  }

  @Getter
  @NoArgsConstructor
  public static class OpinionItem {
    private String title;
    private String nickname;
    private String content;

    public OpinionItem(FairView fairView, User user) {
      String nickname = user==null ? fairView.getNickname() : user.getNickname();
      this.content = decodeHtml(fairView.getContent());
      this.title = decodeHtml(fairView.getTitle());
      this.nickname = nickname;
    }

    public static List<OpinionItem> fromPairViews(
        List<FairView> fairViews, Map<Long, User> userMap) {
      if (fairViews == null) {
        return new ArrayList<>();
      }
      List<OpinionItem> opinionItems = new ArrayList<>();
      for (FairView fairView : fairViews) {
        opinionItems.add(new OpinionItem(fairView, userMap.get(fairView.getUserId())));
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
    private List<TagDto.Request> tags;

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
    private int voteCount = 0;
    private int index = 0;
    private boolean voted = false;

    public PollItem(PollOption polloption, boolean voted) {
      this.pollOptionId = polloption.getId();
      this.name = decodeHtml(polloption.getName());
      this.voteCount = polloption.getVoteCount();
      this.index = polloption.getIndex();
        this.voted = voted;
    }

    public static List<PollItem> fromPollOption(List<PollOption> pollOptions, List<Long> votedOptionIds) {
      List<PollItem> pollList = new ArrayList<>();
      for (PollOption polloption : pollOptions) {
        boolean voted = votedOptionIds != null && votedOptionIds.contains(polloption.getId());
        pollList.add(new PollItem(polloption,voted));
      }
      return pollList;
    }
  }

  public record PostListDto(
          Long postId,
          String title,
          String content,
          String nickname,
          LocalDateTime createdAt,
          Long likeCount,
          Long commentCount,
          Long viewCount,
          Boolean likedByUser
  ) {}

}
