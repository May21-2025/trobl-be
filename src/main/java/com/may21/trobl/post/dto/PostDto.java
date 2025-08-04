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
import com.may21.trobl.redis.RedisDto;
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
import java.util.Objects;

import static com.may21.trobl._global.utility.SecurityUtils.decodeHtml;
import static com.may21.trobl._global.utility.SecurityUtils.escapeHtml;

public class PostDto {

    @Getter
    @AllArgsConstructor
    public static class BasePostDto {
        private final Long postId;
        private final UserDto.Info user;
        private final String title;
        private final List<TagDto.Response> tags;
        private final int viewCount;

        public BasePostDto(Posting post, User user, List<Tag> tags) {
            this.postId = post.getId();
            this.user = user == null ? null : new UserDto.Info(user);
            this.viewCount = post.getAllViewCount();
            this.title = decodeHtml(post.getTitle());
            this.tags = TagDto.Response.fromTagList(tags);
        }

        public BasePostDto(RedisDto.PostDto postDto, RedisDto.UserDto userDto, List<Tag> tags,
                int view) {
            this.postId = postDto.getPostId();
            this.user = userDto == null ? null : new UserDto.Info(userDto);
            this.viewCount = postDto.getViewCount() + view;
            this.title = decodeHtml(postDto.getTitle());
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
            this.user = user == null ? null : new UserDto.Info(user);
            this.viewCount = post.getAllViewCount();
            this.title = decodeHtml(post.getTitle());

            this.commentCount = commentCount;
            this.likeCount = post.getLikeCount();
        }
    }

    @Getter
    public static class RequestedListItem extends BasePostDto {
        private final String content;
        private final PostingType postType;
        private final LocalDateTime createdAt;
        private final boolean newRequest;
        private final boolean confirmed;

        public RequestedListItem(Posting post, User user, List<Tag> tags, boolean newRequest) {
            super(post, user, tags);
            this.content = decodeHtml(post.getContent());
            this.postType = post.getPostType();
            this.createdAt = post.getCreatedAt();
            this.newRequest = newRequest;
            if (post.getPostType() == PostingType.FAIR_VIEW) {
                this.confirmed = post.isConfirmed();
            }
            else {
                this.confirmed = true;
            }
        }
    }

    @Getter
    public static class ListItem extends BasePostDto {
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
    public static class RequestedItem extends BasePostDto {
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
            this.confirmed = post.isConfirmed();
            this.content = decodeHtml(post.getContent());
            this.createdAt = post.getCreatedAt();
            this.postType = post.getPostType();
            this.fairViewStatus = FairViewStatus.getStatusByEntity(fairView);
        }
    }

    @Getter
    public static class QuickPoll {
        private final Long postId;
        private final UserDto.Info user;
        private final String title;
        private final Long userId;
        private final int voteCount;
        private final PollDto poll;
        private final LocalDateTime createdAt;


        public QuickPoll(Posting post, User user, List<Long> votedOptionIds, boolean isOwner) {
            Poll poll = post.getPoll();
            this.postId = post.getId();
            this.user = user == null ? null : new UserDto.Info(user);
            this.title = decodeHtml(poll.getTitle());
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

        public QuickPoll(Posting posting, RedisDto.PollDto pollDto, RedisDto.UserDto userDto,
                List<RedisDto.PollOptionDto> pollOptionDtos, List<Long> votedOptionIds,
                int voteCount, Long userId) {
            this.postId = posting.getId();
            this.user = userDto == null ? null : new UserDto.Info(userDto);
            this.title = decodeHtml(pollDto.getTitle());
            this.userId = posting.getUserId();
            this.createdAt = posting.getCreatedAt();
            this.poll = new PollDto(pollDto, Objects.equals(userId, posting.getUserId()),
                    pollOptionDtos, votedOptionIds);
            this.voteCount = posting.getViewCount() + voteCount;
        }
    }

    @Getter
    public static class Detail extends BasePostDto {
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
        private final boolean confirmed;

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
            this.confirmed = post.isConfirmed();
        }

        public Detail(RedisDto.PostDto postDto, List<RedisDto.FairViewDto> fairViews,
                RedisDto.PollDto pollDto, List<RedisDto.PollOptionDto> optionDtoList,
                Map<Long, RedisDto.UserDto> userMap, List<Tag> tags, boolean liked,
                boolean bookmarked, List<Long> votedOptionIds, boolean isOwner) {
            super(postDto, userMap.get(postDto.getUserId()), tags, 0);
            this.userId = postDto.getUserId();
            this.createdAt = postDto.getCreatedAtAsLocalDateTime(); // String을 LocalDateTime으로 변환

            // PollDto 생성
            if (pollDto != null && optionDtoList != null) {
                List<PollItem> pollItems = new ArrayList<>();
                for (RedisDto.PollOptionDto optionDto : optionDtoList) {
                    boolean voted = votedOptionIds != null &&
                            votedOptionIds.contains(optionDto.getPollOptionId());
                    pollItems.add(new PollItem(optionDto.getPollOptionId(), optionDto.getName(),
                            optionDto.getVoteCount(), optionDto.getIndex(), voted));
                }

                boolean hasVoted = pollItems.stream()
                        .anyMatch(PollItem::isVoted);
                this.poll = new PollDto(pollDto.getPollId(), pollDto.getTitle(),
                        pollDto.isAllowMultipleVotes(), isOwner || hasVoted, pollItems);
            }
            else {
                this.poll = null;
            }

            // FairViewItems 생성
            this.fairViewItems = fairViews == null ? null : fairViews.stream()
                    .map(fairView -> new FairViewItem(fairView.getFairViewId(),
                            fairView.getUserId(), fairView.getTitle(), fairView.getNickname(),
                            fairView.getContent()))
                    .toList();

            this.shareCount = postDto.getShareCount();
            this.postType = postDto.getPostType()
                    .name();
            this.content = decodeHtml(postDto.getContent());
            this.liked = liked;
            this.bookmarked = bookmarked;
            this.commentCount = 0; // RedisDto에는 comment 정보가 없으므로 0으로 설정
            this.likeCount = 0; // RedisDto에는 like 정보가 없으므로 0으로 설정
            this.confirmed = postDto.isConfirmed();
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

        // Constructor for cached data
        public PollDto(Long pollId, String title, boolean allowMultipleVotes,
                boolean showPollResult, List<PollItem> pollOptions) {
            this.pollId = pollId;
            this.title = title;
            this.allowMultipleVotes = allowMultipleVotes;
            this.showPollResult = showPollResult;
            this.pollOptions = pollOptions;
        }

        public PollDto(RedisDto.PollDto pollDto, boolean isOwner,
                List<RedisDto.PollOptionDto> pollOptionDtos, List<Long> votedOptionIds) {
            this.pollId = pollDto.getPollId();
            this.title = pollDto.getTitle();
            this.allowMultipleVotes = pollDto.isAllowMultipleVotes();
            this.pollOptions = PollItem.fromPollOptionDto(pollOptionDtos, votedOptionIds);
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

        public PollItem(RedisDto.PollOptionDto pollOption, boolean voted) {
            this.pollOptionId = pollOption.getPollOptionId();
            this.name = decodeHtml(pollOption.getName());
            this.voteCount = pollOption.getVoteCount();
            this.index = pollOption.getIndex();
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

        public static List<PollItem> fromPollOptionDto(List<RedisDto.PollOptionDto> pollOptionDtos,
                List<Long> votedOptionIds) {
            List<PollItem> pollList = new ArrayList<>();
            for (RedisDto.PollOptionDto pollOptionDto : pollOptionDtos) {
                boolean voted = votedOptionIds != null &&
                        votedOptionIds.contains(pollOptionDto.getPollOptionId());
                pollList.add(new PollItem(pollOptionDto, voted));
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

    @Getter
    public static class HotFairView {
        private final Long postId;
        private final PostingType postType;
        private final String title;
        private final String content;
        private final long viewCount;
        private final long commentCount;
        private final long likeCount;
        private final List<FairViewItem> fairViewItems;
        private final LocalDateTime createdAt;
        private final boolean liked;

        public HotFairView(Posting post, List<FairViewItem> fairViewItems, boolean liked) {
            this.postId = post.getId();
            this.postType = post.getPostType();
            this.title = decodeHtml(post.getTitle());
            this.content = decodeHtml(post.getContent());
            this.viewCount = post.getAllViewCount();
            this.commentCount = post.getCommentCount();
            this.likeCount = post.getLikeCount();
            this.fairViewItems = fairViewItems;
            this.createdAt = post.getCreatedAt();
            this.liked = liked;
        }
    }
}
