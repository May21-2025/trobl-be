package com.may21.trobl.post.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.may21.trobl._global.enums.FairViewStatus;
import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.notification.dto.NotificationDto;
import com.may21.trobl.poll.domain.Poll;
import com.may21.trobl.poll.domain.PollOption;
import com.may21.trobl.post.domain.FairView;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.may21.trobl._global.utility.SecurityUtils.decodeHtml;
import static com.may21.trobl._global.utility.SecurityUtils.escapeHtml;

public class PostDto {

    @Getter
    @AllArgsConstructor
    public static class BasePostDto {
        private final Long postId;
        private final UserDto.Info user;
        private final int viewCount;

        public BasePostDto(Posting post, User user) {
            this.postId = post.getId();
            this.user = user == null ? new UserDto.Info(post) : new UserDto.Info(user);
            this.viewCount = post.getViewCount();
        }
    }

    @Getter
    public static class BasicPostWithTitle extends BasePostDto {
        private final String title;
        private final List<TagDto.Response> tags;

        public BasicPostWithTitle(Posting post, User user, List<Tag> tags) {
            super(post, user);
            this.title = decodeHtml(post.getTitle());
            this.tags = TagDto.Response.fromTagList(tags);
        }
    }


    @Getter
    public static class Card {
        private final Long postId;
        private final UserDto.Info user;
        private final int viewCount;
        private final String title;
        private final int commentCount;
        private final int likeCount;

        @JsonCreator
        public Card(@JsonProperty("postId") Long postId, @JsonProperty("user") UserDto.Info user,
                @JsonProperty("viewCount") int viewCount, @JsonProperty("title") String title,
                @JsonProperty("commentCount") int commentCount,
                @JsonProperty("likeCount") int likeCount) {
            this.postId = postId;
            this.user = user;
            this.viewCount = viewCount;
            this.title = title;
            this.commentCount = commentCount;
            this.likeCount = likeCount;
        }

        public Card(Posting post, int commentCount, User user) {
            this.postId = post.getId();
            this.user = user == null ? new UserDto.Info(post) : new UserDto.Info(user);
            this.viewCount = post.getViewCount();
            this.title = decodeHtml(post.getTitle());

            this.commentCount = commentCount;
            this.likeCount = post.getLikeCount();
        }
    }

    @Getter
    public static class RequestedListItem extends BasicPostWithTitle {
        private final String content;
        private final PostingType postType;
        private final LocalDateTime createdAt;
        private final boolean newRequest;

        public RequestedListItem(Posting post, User user, List<Tag> tags, boolean newRequest) {
            super(post, user, tags);
            this.content = decodeHtml(post.getContent());
            this.postType = post.getPostType();
            this.createdAt = post.getCreatedAt();
            this.newRequest = newRequest;
        }
    }

    @Getter
    public static class ListItem extends BasicPostWithTitle {
        private final String content;
        private final LocalDateTime createdAt;
        private final int commentCount;
        private final int likeCount;
        private final boolean liked;
        private final boolean viewed;
        private final boolean commented;
        private final PostingType postType;

        public ListItem(Posting post, User user, List<Tag> tags, Boolean liked, Boolean viewed,
                Boolean commented) {
            super(post, user, tags);
            this.createdAt = post.getCreatedAt();
            this.commentCount = post.getCommentCount();
            this.likeCount = post.getLikeCount();
            this.liked = Boolean.TRUE.equals(liked);
            this.viewed = Boolean.TRUE.equals(viewed);
            this.commented = Boolean.TRUE.equals(commented);
            this.postType = post.getPostType();

            String raw = decodeHtml(post.getContent());
            this.content =
                    raw == null ? "" : raw.length() > 100 ? raw.substring(0, 100) + "..." : raw;
        }
    }

    @Getter
    public static class MyListItem extends ListItem {
        private final boolean unread;
        private final boolean newComment;
        private final boolean newLike;

        public MyListItem(Posting post, User user, List<Tag> tags, Boolean liked, Boolean viewed,
                Boolean commented, NotificationDto.ContentUpdateStatus contentUpdateStatus) {
            super(post, user, tags, liked, viewed, commented);
            this.unread = contentUpdateStatus.isUnread();
            this.newComment = contentUpdateStatus.isNewComment();
            this.newLike = contentUpdateStatus.isNewLike();
        }
    }

    @Getter
    public static class RequestedItem extends BasicPostWithTitle {
        private final boolean unread;
        private final boolean confirmed;
        private final String content;
        private final LocalDateTime createdAt;
        private final PostingType postType;
        private final FairViewStatus fairViewStatus;

        public RequestedItem(Posting post, User user, List<Tag> tags,
                NotificationDto.ContentUpdateStatus contentUpdateStatus, FairView fairView) {
            super(post, user, tags);
            this.unread = contentUpdateStatus.isUnread();
            this.confirmed = post.getConfirmed();
            this.content = decodeHtml(post.getContent());
            this.createdAt = post.getCreatedAt();
            this.postType = post.getPostType();
            this.fairViewStatus = FairViewStatus.getStatusByEntity(fairView);
        }
    }

    @Getter
    public static class QuickPoll extends BasePostDto {
        private final Long userId;
        private final LocalDateTime createdAt;
        private final int voteCount;
        private final PollDto poll;

        public QuickPoll(Posting post, List<Long> votedOptionIds, boolean isOwner) {
            super(post, null);
            Poll poll = post.getPoll();
            this.userId = post.getUserId();
            this.createdAt = post.getCreatedAt();
            PollDto pollDto = new PollDto(poll, votedOptionIds, isOwner);
            this.poll = pollDto;
            int voteCount = 0;
            for (PollItem pollItem : pollDto.getPollOptions()) {
                voteCount += pollItem.getVoteCount();
            }
            this.voteCount = voteCount;
        }
    }

    @Getter
    public static class Detail extends BasicPostWithTitle {
        private final Long userId;
        private final LocalDateTime createdAt;
        private final PollDto poll;
        private final List<FairViewItem> fairViewItems;
        private final String content;
        private final int shareCount;
        private final boolean liked;
        private final boolean bookmarked;
        private final String postType;
        private final int commentCount;
        private final int likeCount;

        public Detail(Posting post, User user, Map<Long, User> userMap, List<Tag> tags,
                boolean liked, boolean bookmarked, List<Long> postIds, boolean isOwner) {
            super(post, user, tags);
            this.userId = post.getUserId();
            this.createdAt = post.getCreatedAt();
            this.poll =
                    post.getPoll() != null ? new PollDto(post.getPoll(), postIds, isOwner) : null;
            this.fairViewItems = FairViewItem.fromFairViews(post.getFairViews(), userMap);
            this.shareCount = post.getShareCount();
            this.postType = post.getPostType()
                    .name();
            this.content = decodeHtml(post.getContent());
            this.liked = liked;
            this.bookmarked = bookmarked;
            this.commentCount = post.getComments()
                    .size();
            this.likeCount = post.getPostLikes()
                    .size();
        }

        public void blindPartnerContent(Long userId) {
            for (FairViewItem fairViewItem : fairViewItems) {
                if (!fairViewItem.getUserId()
                        .equals(userId)) {
                    fairViewItem.setContent(null);
                    break;
                }
            }
        }
    }

    @Getter
    public static class PollDto {
        private final Long pollId;
        private final boolean allowMultipleVotes;
        private final String title;
        private final boolean showPollResult;
        private final List<PollItem> pollOptions;

        public PollDto(Poll poll, List<Long> votedOptionIds, boolean isOwner) {
            this.pollId = poll.getId();
            this.title = poll.getTitle();
            this.allowMultipleVotes = poll.isAllowedMultipleVotes();
            this.pollOptions = PollItem.fromPollOption(poll.getPollOptions(), votedOptionIds);

            boolean hasVoted = false;
            for (PollItem pollItem : pollOptions) {
                if (pollItem.isVoted()) {
                    hasVoted = true;
                    break;
                }

            }
            this.showPollResult = isOwner || hasVoted;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class PollRequest {
        private final Long pollId;
        private final String title;
        private final boolean allowMultipleVotes;
        private final List<PollOptionRequest> pollOptions;
    }

    public record PollOptionRequest(Long pollOptionId, String name, int index) {
        //getter
        public Long getPollOptionId() {
            return pollOptionId;
        }

        public String getName() {
            return escapeHtml(name);
        }

        public int getIndex() {
            return index;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class FairViewRequest {
        private final String title;
        private final String content;
    }

    @Getter
    public static class FairViewItem {
        private final Long fairViewId;
        private final Long userId;
        private final String title;
        private final String nickname;
        @Setter
        private String content;
        private final boolean confirmed;

        @JsonCreator
        public FairViewItem(@JsonProperty("fairViewId") Long fairViewId,
                @JsonProperty("userId") Long userId, @JsonProperty("title") String title,
                @JsonProperty("nickname") String nickname,
                @JsonProperty("content") String content) {
            this.fairViewId = fairViewId;
            this.userId = userId;
            this.title = title;
            this.content = content;
            this.nickname = nickname;
            this.confirmed = false;
        }

        public FairViewItem(FairView fairView, User user) {
            String nickname = user == null ? fairView.getNickname() : user.getNickname();
            this.fairViewId = fairView.getId();
            this.userId = fairView.getUserId();
            this.content = decodeHtml(fairView.getContent());
            this.title = decodeHtml(fairView.getTitle());
            this.nickname = nickname;
            this.confirmed = fairView.isConfirmed();
        }

        public static List<FairViewItem> fromFairViews(List<FairView> fairViews,
                Map<Long, User> userMap) {
            if (fairViews == null) {
                return new ArrayList<>();
            }
            List<FairViewItem> fairViewItems = new ArrayList<>();
            for (FairView fairView : fairViews) {
                fairViewItems.add(new FairViewItem(fairView, userMap.get(fairView.getUserId())));
            }
            return fairViewItems;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Request {
        private final String title;
        private final String content;
        private final String postType;
        private final FairViewItem fairViewItem;
        private final PollRequest poll;
        private final List<TagDto.Request> tags;

        public String getTitle() {
            return escapeHtml(title);
        }

        public String getPollTitle() {
            return poll == null ? "" : escapeHtml(poll.title);
        }

        public Long getPollId() {
            return poll == null ? null : poll.pollId;
        }

        public String getContent() {
            return escapeHtml(content);
        }

        public boolean isAllowMultipleVotes() {
            return poll.isAllowMultipleVotes();
        }

    }

    @Getter
    @Setter
    public static class PollItem {
        private final Long pollOptionId;
        private final String name;
        private int voteCount = 0;
        private int index = 0;
        private boolean voted = false;

        @JsonCreator
        public PollItem(@JsonProperty("pollOptionId") Long pollOptionId,
                @JsonProperty("name") String name, @JsonProperty("voteCount") int voteCount,
                @JsonProperty("index") int index, @JsonProperty("voted") boolean voted) {
            this.pollOptionId = pollOptionId;
            this.name = name;
            this.voteCount = voteCount;
            this.index = index;
            this.voted = voted;
        }

        public PollItem(PollOption polloption, boolean voted) {
            this.pollOptionId = polloption.getId();
            this.name = decodeHtml(polloption.getName());
            this.voteCount = polloption.getVoteCount();
            this.index = polloption.getIndex();
            this.voted = voted;
        }

        public static List<PollItem> fromPollOption(List<PollOption> pollOptions,
                List<Long> votedOptionIds) {
            List<PollItem> pollList = new ArrayList<>();
            for (PollOption polloption : pollOptions) {
                boolean voted =
                        votedOptionIds != null && votedOptionIds.contains(polloption.getId());
                pollList.add(new PollItem(polloption, voted));
            }
            return pollList;
        }
    }

    public record PostListDto(Long postId, String title, String content, String nickname,
                              LocalDateTime createdAt, Long likeCount, Long commentCount,
                              Long viewCount, Boolean likedByUser) {}


    @Getter
    public static class Notification {
        private final Long postId;
        private final String title;
        private final LocalDateTime createdAt;

        public Notification(Posting post) {
            this.postId = post.getId();
            this.title = decodeHtml(post.getTitle());
            this.createdAt = post.getCreatedAt();

        }
    }
}
