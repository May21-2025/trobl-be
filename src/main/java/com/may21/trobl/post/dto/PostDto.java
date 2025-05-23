package com.may21.trobl.post.dto;

import static com.may21.trobl._global.utility.SecurityUtils.decodeHtml;
import static com.may21.trobl._global.utility.SecurityUtils.escapeHtml;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.post.domain.PairView;
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
  public static class ListItem {
    private Long postId;
    private String title;
    private String username;
    private LocalDateTime createdAt;

    public ListItem(Posting post, User user) {
      this.postId = post.getId();
      this.title = post.getTitle();
      this.username = user.getNickname();
      this.createdAt = post.getCreatedAt();
    }

    public ListItem(Posting post) {
      this.postId = post.getId();
      this.title = post.getTitle();
      this.username = post.getNickname();
      this.createdAt = post.getCreatedAt();
    }
  }

  @Getter
  @NoArgsConstructor
  public static class View {
    private Long postId;
    private Long userId;
    private String username;
    private int viewCount;
    private int shareCount;
    private String postType;
    private LocalDateTime createdAt;
    private Poll poll;
    private List<OpinionItem> opinions;

    public View(Posting posting, User user, Map<Long, User> userMap) {
      this.userId = user.getId();
      this.postId = posting.getId();
      this.postType = posting.getPostType().name();
      this.username = user.getNickname();
      this.viewCount = posting.getViewCount();
      this.shareCount = posting.getShareCount();
      this.createdAt = posting.getCreatedAt();
      this.poll = posting.getPostType() == PostingType.POLL ? new Poll(posting.getPollTitle(), posting.getPollOptions()): null;
      this.opinions = posting.getPostType() == PostingType.PAIR_VIEW ? OpinionItem.fromPairViews(posting.getPairViews(), userMap) : List.of();
    }
  }

  @Getter
  @NoArgsConstructor
  public static class Poll {
    private String title;
    private List<PollItem> pollOptions;

    public Poll(String title, List<PollOption> options) {
      this.title = decodeHtml(title);
      this.pollOptions = PollItem.fromPollOption(options);
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
  public static class Detail extends View {
    private String title;
    private String content;

    public Detail(Posting posting, User user, Map<Long, User> userMap) {
      super(posting, user, userMap);
      this.title = decodeHtml(posting.getTitle());
      this.content = decodeHtml(posting.getContent());
    }
  }

  @Getter
  @NoArgsConstructor
  public static class Request {
    private String title;
    private String content;
    private String postType;
    private OpinionItem optionItem;
    private Poll poll;

    public String getTitle() {
      return escapeHtml(title);
    }

    public String getPollTitle() {
      return poll==null? "" : escapeHtml(poll.title);
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
