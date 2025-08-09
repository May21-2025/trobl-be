package com.may21.trobl.redis;

import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.poll.domain.Poll;
import com.may21.trobl.poll.domain.PollOption;
import com.may21.trobl.poll.repository.PollOptionRepository;
import com.may21.trobl.poll.repository.PollRepository;
import com.may21.trobl.post.domain.FairView;
import com.may21.trobl.post.domain.FairViewRepository;
import com.may21.trobl.post.domain.PostRepository;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.repository.TagMappingRepository;
import com.may21.trobl.tag.repository.TagRepository;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final FairViewRepository fairViewRepository;

    private static final String POST_CACHE_KEY = "post:";
    private static final String USER_CACHE_KEY = "user:";
    private static final String TAG_CACHE_KEY = "tag:";
    private static final String TAG_LIST_CACHE_KEY = "tag_list:";
    private static final String POLL_CACHE_KEY = "poll:";
    private static final String POLL_OPTION_CACHE_KEY = "poll_option:";
    private static final String FAIR_VIEW_CACHE_KEY = "fair_view:";
    private static final String POST_POLL_MAPPING_KEY = "post_poll_mapping:";
    private static final String POLL_OPTION_LIST_KEY = "poll_option_list:";
    private static final String FAIR_VIEW_LIST_KEY = "fair_view_list:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);

    /**
     * 포스트 정보를 Redis에서 가져오거나, 없으면 DB에서 가져와서 Redis에 저장
     * FairView와 Poll 관련 데이터도 함께 캐시
     */
    public RedisDto.PostDto getPostFromCache(Long postId) {
        try {
            String key = POST_CACHE_KEY + postId;
            RedisDto.PostDto cachedPostDto = (RedisDto.PostDto) redisTemplate.opsForValue()
                    .get(key);

            if (cachedPostDto != null) {return cachedPostDto;}

            // Redis에 없으면 DB에서 가져오기
            Posting post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
            RedisDto.PostDto postDto = convertToRedisPost(post);

            // Redis에 저장
            redisTemplate.opsForValue()
                    .set(key, postDto, DEFAULT_TTL);
            log.info("Post {} loaded from DB and cached", postId);

            return postDto;
        } catch (Exception e) {
            log.error("Error getting post from cache: {}", e.getMessage());
            Posting post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(ExceptionCode.POST_NOT_FOUND));
            return convertToRedisPost(post);
        }
    }
    /**
     * 사용자 정보를 Redis에서 가져오거나, 없으면 DB에서 가져와서 Redis에 저장
     */
    public RedisDto.UserDto getUserFromCache(Long userId) {
        try {
            String key = USER_CACHE_KEY + userId;
            RedisDto.UserDto cachedUserDto = (RedisDto.UserDto) redisTemplate.opsForValue()
                    .get(key);

            if (cachedUserDto != null) {
                return cachedUserDto;
            }

            // Redis에 없으면 DB에서 가져오기
            User user = userRepository.findById(userId)
                    .orElse(null);
            RedisDto.UserDto userDto = convertToRedisUser(user);

            // Redis에 저장
            redisTemplate.opsForValue()
                    .set(key, userDto, DEFAULT_TTL);
            log.info("User {} loaded from DB and cached", userId);

            return userDto;
        } catch (Exception e) {
            log.error("Error getting user from cache: {}", e.getMessage());
            // Redis 에러 시 DB에서 직접 조회
            return null;
        }
    }

    /**
     * 태그 정보를 Redis에서 가져오거나, 없으면 DB에서 가져와서 Redis에 저장
     */
    public Optional<RedisDto.TagDto> getTagFromCache(Long tagId) {
        try {
            String key = TAG_CACHE_KEY + tagId;
            RedisDto.TagDto cachedTagDto = (RedisDto.TagDto) redisTemplate.opsForValue()
                    .get(key);

            if (cachedTagDto != null) {
                log.info("Tag {} found in cache", tagId);
                return Optional.of(cachedTagDto);
            }

            // Redis에 없으면 DB에서 가져오기
            Optional<Tag> tag = tagRepository.findById(tagId);
            if (tag.isPresent()) {
                Tag tagEntity = tag.get();
                RedisDto.TagDto tagDto = convertToRedisTag(tagEntity);

                // Redis에 저장
                redisTemplate.opsForValue()
                        .set(key, tagDto, DEFAULT_TTL);
                log.info("Tag {} loaded from DB and cached", tagId);

                return Optional.of(tagDto);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Error getting tag from cache: {}", e.getMessage());
            // Redis 에러 시 DB에서 직접 조회
            return tagRepository.findById(tagId)
                    .map(this::convertToRedisTag);
        }
    }

    /**
     * Poll 정보를 Redis에서 가져오거나, 없으면 DB에서 가져와서 Redis에 저장
     */
    public RedisDto.PollDto getPollFromCache(Long postId) {
        try {
            // postId -> pollId 매핑을 통해 pollId 찾기
            String mappingKey = POST_POLL_MAPPING_KEY + postId;
            Long pollId = (Long)redisTemplate.opsForValue()
                    .get(mappingKey);

            if (pollId != null) {
                String pollKey = POLL_CACHE_KEY + pollId;
                RedisDto.PollDto cachedPollDto = (RedisDto.PollDto) redisTemplate.opsForValue()
                        .get(pollKey);
                if (cachedPollDto != null) {
                    log.info("Poll for post {} found in cache", postId);
                    return cachedPollDto;
                }
            }

            // Redis에 없으면 DB에서 가져오기
            Optional<Poll> pollOpt = pollRepository.findByPostingId(postId);
            if (pollOpt.isEmpty()) {
                return null;
            }

            Poll poll = pollOpt.get();
            RedisDto.PollDto pollDto = convertToRedisPoll(poll);
            String pollKey = POLL_CACHE_KEY + poll.getId();

            // Redis에 저장
            redisTemplate.opsForValue()
                    .set(pollKey, pollDto, DEFAULT_TTL);

            // postId -> pollId 매핑 저장
            redisTemplate.opsForValue()
                    .set(mappingKey, poll.getId(), DEFAULT_TTL);

            log.info("Poll for post {} loaded from DB and cached", postId);

            return pollDto;

        } catch (Exception e) {
            log.error("Error getting poll from cache: {}", e.getMessage());
            // Redis 에러 시 DB에서 직접 조회
            return null;
        }
    }

    /**
     * PollOption 정보를 Redis에서 가져오거나, 없으면 DB에서 가져와서 Redis에 저장
     */
    public List<RedisDto.PollOptionDto> getPollOptionFromCache(Long postId) {
        try {
            // pollId -> pollOptionIds 매핑으로 pollOptionIds 찾기
            String pollOptionListKey = POLL_OPTION_LIST_KEY + postId;
            @SuppressWarnings("unchecked") List<Long> pollOptionIds =
                    (List<Long>) redisTemplate.opsForValue()
                            .get(pollOptionListKey);

            if (pollOptionIds != null && !pollOptionIds.isEmpty()) {
                // 각 pollOptionId로 개별 PollOption 데이터 조회
                List<RedisDto.PollOptionDto> pollOptionDtos = new ArrayList<>();
                for (Long optionId : pollOptionIds) {
                    String optionKey = POLL_OPTION_CACHE_KEY + optionId;
                    RedisDto.PollOptionDto optionDto =
                            (RedisDto.PollOptionDto) redisTemplate.opsForValue()
                                    .get(optionKey);
                    if (optionDto != null) {
                        pollOptionDtos.add(optionDto);
                    }
                }

                if (!pollOptionDtos.isEmpty()) {
                    log.info("PollOptions for poll {} found in cache", postId);
                    return pollOptionDtos;
                }
            }

            // Redis에 없으면 DB에서 가져오기
            List<PollOption> pollOptionList = pollOptionRepository.findByPostId(postId);
            List<RedisDto.PollOptionDto> optionDtos = new ArrayList<>();
            for (PollOption option : pollOptionList) {
                RedisDto.PollOptionDto optionDto = convertToRedisPollOption(postId, option);
                optionDtos.add(optionDto);
            }

            // 개별 PollOption 데이터 저장
            pollOptionIds = new ArrayList<>();
            for (PollOption option : pollOptionList) {
                RedisDto.PollOptionDto optionDto = convertToRedisPollOption(postId, option);
                String optionKey = POLL_OPTION_CACHE_KEY + option.getId();
                redisTemplate.opsForValue()
                        .set(optionKey, optionDto, DEFAULT_TTL);
                pollOptionIds.add(option.getId());
            }

            // pollId -> pollOptionIds 매핑 저장
            redisTemplate.opsForValue()
                    .set(pollOptionListKey, pollOptionIds, DEFAULT_TTL);
            log.info("PollOptions for poll {} loaded from DB and cached", postId);

            return optionDtos;
        } catch (Exception e) {
            log.error("Error getting poll options from cache: {}", e.getMessage());
            // Redis 에러 시 DB에서 직접 조회
            List<PollOption> pollOptionList = pollOptionRepository.findByPostId(postId);
            List<RedisDto.PollOptionDto> optionDtos = new ArrayList<>();
            for (PollOption option : pollOptionList) {
                RedisDto.PollOptionDto optionDto = convertToRedisPollOption(postId, option);
                optionDtos.add(optionDto);
            }
            return optionDtos;
        }
    }

    /**
     * FairView 정보를 Redis에서 가져오거나, 없으면 DB에서 가져와서 Redis에 저장
     */
    public List<RedisDto.FairViewDto> getFairViewFromCache(Long postId) {
        try {
            String key = FAIR_VIEW_LIST_KEY + postId;
            @SuppressWarnings("unchecked") List<RedisDto.FairViewDto> cachedFairViewDtos =
                    (List<RedisDto.FairViewDto>) redisTemplate.opsForValue()
                            .get(key);

            if (cachedFairViewDtos != null) {
                log.info("FairViews for post {} found in cache", postId);
                return cachedFairViewDtos;
            }

            // Redis에 없으면 DB에서 가져오기
            List<FairView> fairViews = fairViewRepository.findByPostingId(postId);

            List<RedisDto.FairViewDto> fairViewDtoList = fairViews.stream()
                    .map(this::convertToRedisFairView)
                    .collect(Collectors.toList());
            // Redis에 저장
            redisTemplate.opsForValue()
                    .set(key, fairViewDtoList, DEFAULT_TTL);
            log.info("FairViews for post {} loaded from DB and cached", postId);

            return fairViewDtoList;
        } catch (Exception e) {
            log.error("Error getting fair view from cache: {}", e.getMessage());
            // Redis 에러 시 DB에서 직접 조회
            Optional<Posting> posting = postRepository.findById(postId);
            if (posting.isPresent() && posting.get()
                    .getFairViews() != null) {
                return posting.get()
                        .getFairViews()
                        .stream()
                        .map(this::convertToRedisFairView)
                        .collect(Collectors.toList());
            }
            return List.of();
        }
    }


    /**
     * 캐시에서 데이터 삭제
     */
    public void evictPostFromCache(Long postId) {
        try {
            String key = POST_CACHE_KEY + postId;
            redisTemplate.delete(key);

            // 관련 데이터도 함께 삭제
            evictPostRelatedData(postId);

            log.info("Post {} and related data evicted from cache", postId);
        } catch (Exception e) {
            log.error("Error evicting post from cache: {}", e.getMessage());
        }
    }

    /**
     * 포스트 관련 데이터 삭제
     */
    private void evictPostRelatedData(Long postId) {
        try {
            // Poll 관련 데이터 삭제
            Optional<Poll> poll = pollRepository.findByPostingId(postId);
            if (poll.isPresent()) {
                String pollKey = POLL_CACHE_KEY + poll.get()
                        .getId();
                redisTemplate.delete(pollKey);

                // PollOption 데이터 삭제
                if (poll.get()
                        .getPollOptions() != null) {
                    for (PollOption option : poll.get()
                            .getPollOptions()) {
                        String optionKey = POLL_OPTION_CACHE_KEY + option.getId();
                        redisTemplate.delete(optionKey);
                    }
                }

                String pollOptionListKey = POLL_OPTION_LIST_KEY + poll.get()
                        .getId();
                redisTemplate.delete(pollOptionListKey);

                // postId -> pollId 매핑 삭제
                String mappingKey = POST_POLL_MAPPING_KEY + postId;
                redisTemplate.delete(mappingKey);
            }

            // FairView 데이터 삭제
            List<FairView> fairViews = fairViewRepository.findByPostingId(postId);
            for (FairView fairView : fairViews) {
                String fairViewKey = FAIR_VIEW_CACHE_KEY + fairView.getId();
                redisTemplate.delete(fairViewKey);
            }

            String fairViewListKey = "fair_view_list:" + postId;
            redisTemplate.delete(fairViewListKey);

        } catch (Exception e) {
            log.error("Error evicting related data for post {}: {}", postId, e.getMessage());
        }
    }

    public void evictUserFromCache(Long userId) {
        try {
            String key = USER_CACHE_KEY + userId;
            redisTemplate.delete(key);
            log.info("User {} evicted from cache", userId);
        } catch (Exception e) {
            log.error("Error evicting user from cache: {}", e.getMessage());
        }
    }

    public void evictTagFromCache(Long tagId) {
        try {
            String key = TAG_CACHE_KEY + tagId;
            redisTemplate.delete(key);
            log.info("Tag {} evicted from cache", tagId);
        } catch (Exception e) {
            log.error("Error evicting tag from cache: {}", e.getMessage());
        }
    }

    public void evictPostTagsFromCache(Long postId) {
        try {
            String key = TAG_LIST_CACHE_KEY + postId;
            redisTemplate.delete(key);
            log.info("Post tags for {} evicted from cache", postId);
        } catch (Exception e) {
            log.error("Error evicting post tags from cache: {}", e.getMessage());
        }
    }

    public void evictPollFromCache(Long pollId) {
        try {
            String key = POLL_CACHE_KEY + pollId;
            redisTemplate.delete(key);
            log.info("Poll {} evicted from cache", pollId);
        } catch (Exception e) {
            log.error("Error evicting poll from cache: {}", e.getMessage());
        }
    }

    public void evictPollOptionFromCache(Long pollOptionId) {
        try {
            String key = POLL_OPTION_CACHE_KEY + pollOptionId;
            redisTemplate.delete(key);
            log.info("PollOption {} evicted from cache", pollOptionId);
        } catch (Exception e) {
            log.error("Error evicting poll option from cache: {}", e.getMessage());
        }
    }

    public void evictFairViewFromCache(Long fairViewId) {
        try {
            String key = FAIR_VIEW_CACHE_KEY + fairViewId;
            redisTemplate.delete(key);
            log.info("FairView {} evicted from cache", fairViewId);
        } catch (Exception e) {
            log.error("Error evicting fair view from cache: {}", e.getMessage());
        }
    }

    /**
     * 모든 캐시 삭제
     */
    public void clearAllCache() {
        try {
            Objects.requireNonNull(redisTemplate.getConnectionFactory())
                    .getConnection()
                    .flushDb();
            log.info("All cache cleared");
        } catch (Exception e) {
            log.error("Error clearing cache: {}", e.getMessage());
        }
    }

    /**
     * Redis 연결 상태 확인
     */
    public boolean isRedisAvailable() {
        try {
            Objects.requireNonNull(redisTemplate.getConnectionFactory())
                    .getConnection()
                    .ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 캐시 키 존재 여부 확인
     */
    public boolean existsInCache(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Error checking cache key existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 캐시 TTL 조회
     */
    public Long getCacheTTL(String key) {
        try {
            return redisTemplate.getExpire(key);
        } catch (Exception e) {
            log.error("Error getting cache TTL: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 여러 포스트를 한 번에 캐시에서 가져오기
     */
    public List<RedisDto.PostDto> getMultiplePostsFromCache(List<Long> postIds) {
        return postIds.stream()
                .map(this::getPostFromCache)

                .collect(Collectors.toList());
    }

    /**
     * 여러 사용자를 한 번에 캐시에서 가져오기
     */
    public List<RedisDto.UserDto> getMultipleUsersFromCache(List<Long> userIds) {
        return userIds.stream()
                .map(this::getUserFromCache)

                .collect(Collectors.toList());
    }

    /**
     * 캐시 통계 정보
     */
    public void printCacheStats() {
        try {
            log.info("Redis connection available: {}", isRedisAvailable());
            if (isRedisAvailable()) {
                Long dbSize = Objects.requireNonNull(redisTemplate.getConnectionFactory())
                        .getConnection()
                        .dbSize();
                log.info("Total keys in Redis: {}", dbSize);
            }
        } catch (Exception e) {
            log.error("Error getting cache stats: {}", e.getMessage());
        }
    }

    // 변환 메서드들
    private RedisDto.PostDto convertToRedisPost(Posting posting) {
        return RedisDto.PostDto.builder()
                .postId(posting.getId())
                .title(posting.getTitle())
                .userId(posting.getUserId())
                .content(posting.getContent())
                .postType(posting.getPostType())
                .createdAt(posting.getCreatedAt() != null ? posting.getCreatedAt()
                        .toString() : null)
                .confirmed(posting.isConfirmed())
                .viewCount(posting.getViewCount())
                .shareCount(posting.getShareCount())
                .reported(posting.isReported())
                .build();
    }

    private RedisDto.UserDto convertToRedisUser(User user) {
        return new RedisDto.UserDto(user);
    }

    private RedisDto.TagDto convertToRedisTag(Tag tag) {
        RedisDto.TagDto tagDto = new RedisDto.TagDto();
        tagDto.setTagId(tag.getId());
        tagDto.setName(tag.getName());
        return tagDto;
    }

    private RedisDto.PollDto convertToRedisPoll(Poll poll) {

        return RedisDto.PollDto.builder()
                .pollId(poll.getId())
                .postId(poll.getPosting()
                        .getId())
                .allowMultipleVotes(poll.isAllowedMultipleVotes())
                .title(poll.getTitle())
                .build();
    }

    private RedisDto.PollOptionDto convertToRedisPollOption(Long postId, PollOption option) {
        return RedisDto.PollOptionDto.builder()
                .pollOptionId(option.getId())
                .pollId(option.getPoll()
                        .getId())
                .postId(postId)
                .name(option.getName())
                .index(option.getIndex())
                .voteCount(option.getVoteCount())
                .build();
    }

    private RedisDto.FairViewDto convertToRedisFairView(FairView fairView) {
        return RedisDto.FairViewDto.builder()
                .fairViewId(fairView.getId())
                .postId(fairView.getPosting()
                        .getId())
                .userId(fairView.getUserId())
                .title(fairView.getTitle())
                .nickname(fairView.getNickname())
                .content(fairView.getContent())
                .confirmed(fairView.isConfirmed())
                .build();
    }

    public List<PostDto.QuickPoll> getQuickPollDtoByCached(Long userId, List<Posting> posts,
            List<Long> votedOptionIds, Map<Long, Integer> postViewCOuntMap) {

        List<Long> postIds = posts.stream()
                .map(Posting::getId)
                .toList();
        List<Long> userIds = posts.stream()
                .map(Posting::getUserId)
                .toList();
        Map<Long, RedisDto.PollDto> pollDtos = getPollDtoMapByPostIds(postIds);
        Map<Long, RedisDto.UserDto> userDtos = getUserMapFromCache(userIds);
        Map<Long, List<RedisDto.PollOptionDto>> pollOptionDtoMap =
                getPollOptionMapFromPostIds(postIds);

        List<PostDto.QuickPoll> quickPolls = new ArrayList<>();

        for (Posting posting : posts) {
            Long postId = posting.getId();
            RedisDto.PollDto pollDto = pollDtos.get(postId);
            RedisDto.UserDto userDto = userDtos.get(posting.getUserId());
            List<RedisDto.PollOptionDto> pollOptionDtos = pollOptionDtoMap.get(pollDto.getPollId());
            quickPolls.add(
                    new PostDto.QuickPoll(posting, pollDto, userDto, pollOptionDtos, votedOptionIds,
                            postViewCOuntMap.get(postId), userId));

        }
        return quickPolls;
    }

    private Map<Long, List<RedisDto.PollOptionDto>> getPollOptionMapFromPostIds(
            List<Long> postIds) {
        Map<Long, List<RedisDto.PollOptionDto>> pollOptionDtoMap = new HashMap<>();
        for (Long postId : postIds) {
            List<RedisDto.PollOptionDto> pollOptionDtos = getPollOptionFromCache(postId);
            pollOptionDtoMap.put(postId,
                    Objects.requireNonNullElseGet(pollOptionDtos, ArrayList::new));
        }
        return pollOptionDtoMap;
    }

    private Map<Long, RedisDto.UserDto> getUserMapFromCache(List<Long> userIds) {
        return userIds.stream()
                .map(this::getUserFromCache)
                .collect(Collectors.toMap(RedisDto.UserDto::getUserId, Function.identity()));
    }


    private Map<Long, RedisDto.PollDto> getPollDtoMapByPostIds(List<Long> postIds) {
        List<RedisDto.PollDto> pollDtos = new ArrayList<>();
        for (Long postId : postIds) {
            RedisDto.PollDto pollDto = getPollFromCache(postId);
            if (pollDto != null) {
                pollDtos.add(pollDto);
            }
        }
        return pollDtos.stream()
                .collect(Collectors.toMap(RedisDto.PollDto::getPostId, Function.identity()));

    }

    /**
     * 포스트 수정 시 관련 캐시 무효화
     */
    public void invalidatePostCache(Long postId) {
        try {
            // 포스트 캐시 삭제
            evictPostFromCache(postId);

            // 태그 캐시 삭제
            evictPostTagsFromCache(postId);

            log.info("Post cache invalidated for postId: {}", postId);
        } catch (Exception e) {
            log.error("Error invalidating post cache for postId {}: {}", postId, e.getMessage());
        }
    }

    /**
     * Poll 수정 시 관련 캐시 무효화
     */
    public void invalidatePollCache(Long pollId) {
        try {
            // Poll 캐시 삭제
            evictPollFromCache(pollId);

            // PollOption 캐시 삭제
            evictPollOptionFromCache(pollId);

            // PollOption 목록 캐시 삭제
            String pollOptionListKey = POLL_OPTION_LIST_KEY + pollId;
            redisTemplate.delete(pollOptionListKey);

            log.info("Poll cache invalidated for pollId: {}", pollId);
        } catch (Exception e) {
            log.error("Error invalidating poll cache for pollId {}: {}", pollId, e.getMessage());
        }
    }

    /**
     * PollOption 수정 시 관련 캐시 무효화
     */
    public void invalidatePollOptionCache(Long pollOptionId) {
        try {
            // 개별 PollOption 캐시 삭제
            evictPollOptionFromCache(pollOptionId);

            // 해당 PollOption이 속한 Poll의 목록 캐시도 삭제
            // PollOption에서 pollId를 찾아서 해당 Poll의 목록 캐시 삭제
            Optional<PollOption> pollOption = pollOptionRepository.findById(pollOptionId);
            if (pollOption.isPresent()) {
                Long pollId = pollOption.get()
                        .getPoll()
                        .getId();
                String pollOptionListKey = POLL_OPTION_LIST_KEY + pollId;
                redisTemplate.delete(pollOptionListKey);
            }

            log.info("PollOption cache invalidated for pollOptionId: {}", pollOptionId);
        } catch (Exception e) {
            log.error("Error invalidating poll option cache for pollOptionId {}: {}", pollOptionId,
                    e.getMessage());
        }
    }

    /**
     * FairView 수정 시 관련 캐시 무효화
     */
    public void invalidateFairViewCache(Long fairViewId) {
        try {
            // 개별 FairView 캐시 삭제
            evictFairViewFromCache(fairViewId);

            // 해당 FairView가 속한 포스트의 FairView 목록 캐시도 삭제
            Optional<FairView> fairView = fairViewRepository.findById(fairViewId);
            if (fairView.isPresent()) {
                Long postId = fairView.get()
                        .getPosting()
                        .getId();
                String fairViewListKey = "fair_view_list:" + postId;
                redisTemplate.delete(fairViewListKey);
            }

            log.info("FairView cache invalidated for fairViewId: {}", fairViewId);
        } catch (Exception e) {
            log.error("Error invalidating fair view cache for fairViewId {}: {}", fairViewId,
                    e.getMessage());
        }
    }

    /**
     * 사용자 수정 시 관련 캐시 무효화
     */
    public void invalidateUserCache(Long userId) {
        try {
            evictUserFromCache(userId);
            log.info("User cache invalidated for userId: {}", userId);
        } catch (Exception e) {
            log.error("Error invalidating user cache for userId {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 태그 수정 시 관련 캐시 무효화
     */
    public void invalidateTagCache(Long tagId) {
        try {
            evictTagFromCache(tagId);
            log.info("Tag cache invalidated for tagId: {}", tagId);
        } catch (Exception e) {
            log.error("Error invalidating tag cache for tagId {}: {}", tagId, e.getMessage());
        }
    }



    public void invalidatePostRelatedCache(Long postId) {
        try {
            if (postId == null) {
                log.warn("postId is null. Skip cache invalidation.");
                return;
            }

            List<String> postRelatedKeys = List.of(POLL_OPTION_LIST_KEY + postId,
                    POST_POLL_MAPPING_KEY + postId, FAIR_VIEW_LIST_KEY+ postId);
            redisTemplate.delete(postRelatedKeys);
        } catch (Exception e) {
            log.error("Error evicting cache for postId {}: {}", postId, e.getMessage(), e);

        }
    }


}