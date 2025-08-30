package com.may21.trobl.admin.service;

import com.may21.trobl._global.enums.DateType;
import com.may21.trobl._global.enums.PostSortType;
import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl._global.enums.ScheduleType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.admin.domain.LayoutPostMapping;
import com.may21.trobl.admin.domain.MainLayoutGroup;
import com.may21.trobl.admin.repository.LayoutPostMappingRepository;
import com.may21.trobl.admin.repository.MainLayoutRepository;
import com.may21.trobl.admin.repository.PostDetailInfoRepository;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.redis.CacheService;
import com.may21.trobl.redis.RedisDto;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.repository.TagMappingRepository;
import com.may21.trobl.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MainLayoutService {

    private final PostDetailInfoRepository postDetailInfoRepository;
    private final LayoutPostMappingRepository layoutPostMappingRepository;
    private final TagMappingRepository tagMappingRepository;
    private final MainLayoutRepository mainLayoutRepository;
    private final TagService tagService;
    private final CacheService cacheService;


    @Transactional(readOnly = true)
    public Page<AdminDto.MainLayoutInfo> getMainLayoutInfo(int size, int page) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("index")
                .ascending());
        Page<MainLayoutGroup> layouts = mainLayoutRepository.findAll(pageable);
        Set<Long> tagIds = new HashSet<>();
        Map<Long, List<Long>> tagIdMap = new HashMap<>();
        for (MainLayoutGroup layout : layouts) {
            List<Long> tagIdList = layout.getTagIds();
            tagIdMap.putIfAbsent(layout.getId(), tagIdList);
            tagIds.addAll(tagIdList);
        }
        Map<Long, Tag> tagMap = tagService.getLayoutTagMap(tagIds, tagIdMap);
        Map<Long, List<Tag>> layoutTagMap = new HashMap<>();
        for (MainLayoutGroup layout : layouts) {
            Long id = layout.getId();
            List<Tag> tags = new ArrayList<>();
            for (Long tagId : tagIdMap.get(id)) {
                if (tagMap.containsKey(tagId)) {
                    tags.add(tagMap.get(tagId));
                }
            }
            layoutTagMap.put(id, tags);
        }
        return layouts.map(layout -> {
            return new AdminDto.MainLayoutInfo(layout, layoutTagMap.get(layout.getId()));
        });
    }

    @Transactional
    public AdminDto.MainLayoutInfo createMainLayoutInfo(AdminDto.MainLayoutRequest request) {
        int maxIndex = mainLayoutRepository.findMaxIndex();
        MainLayoutGroup layout = new MainLayoutGroup(request, maxIndex);
        layout = mainLayoutRepository.save(layout);
        List<Tag> tags = tagService.getTagsByIds(new HashSet<>(request.getTagIds()));
        setLayoutPosts(layout);
        return new AdminDto.MainLayoutInfo(layout, tags);
    }

    public boolean deleteMainLayoutInfo(Long id) {
        if (!mainLayoutRepository.existsById(id)) {
            throw new BusinessException(ExceptionCode.LAYOUT_NOT_FOUND);
        }
        mainLayoutRepository.deleteById(id);
        return true;
    }

    public boolean changeIndex(Long id, int index) {
        MainLayoutGroup layout = mainLayoutRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExceptionCode.LAYOUT_NOT_FOUND));
        int currentIndex = layout.getIndex();
        if (currentIndex == index) {
            return true; // 변경할 필요 없음
        }
        int maxIndex = mainLayoutRepository.findMaxIndex();
        if (index < 0 || index > maxIndex) {
            throw new BusinessException(ExceptionCode.INVALID_LAYOUT_INDEX);
        }

        if (currentIndex < index) {
            // 아래로 이동: 현재 인덱스보다 크고, 목표 인덱스 이하인 항목들의 인덱스를 -1
            List<MainLayoutGroup> affectedLayouts =
                    mainLayoutRepository.findByIndexBetween(currentIndex + 1, index);
            for (MainLayoutGroup l : affectedLayouts) {
                l.setIndex(l.getIndex() - 1);
            }
            mainLayoutRepository.saveAll(affectedLayouts);
        }
        else {
            // 위로 이동: 현재 인덱스보다 작고, 목표 인덱스 이상인 항목들의 인덱스를 +1
            List<MainLayoutGroup> affectedLayouts =
                    mainLayoutRepository.findByIndexBetween(index, currentIndex - 1);
            for (MainLayoutGroup l : affectedLayouts) {
                l.setIndex(l.getIndex() + 1);
            }
            mainLayoutRepository.saveAll(affectedLayouts);
        }

        // 대상 항목의 인덱스를 목표 인덱스로 설정
        layout.setIndex(index);
        mainLayoutRepository.save(layout);

        return true;

    }

    @Transactional
    public boolean activateLayout(Long id, boolean activation) {
        MainLayoutGroup layout = mainLayoutRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExceptionCode.LAYOUT_NOT_FOUND));
        layout.setActive(activation);
        mainLayoutRepository.save(layout);
        return true;
    }

    @Transactional
    public boolean updateScheduleType(Long id, String scheduleType) {
        MainLayoutGroup layout = mainLayoutRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExceptionCode.LAYOUT_NOT_FOUND));
        ScheduleType type = ScheduleType.fromString(scheduleType);
        layout.setScheduleType(type);
        mainLayoutRepository.save(layout);
        return true;
    }

    @Transactional
    public void setLayoutPosts(MainLayoutGroup mainLayoutGroup) {
        PostSortType sortType = mainLayoutGroup.getSortType();
        DateType dateType = mainLayoutGroup.getDateType();
        Integer dateInt = mainLayoutGroup.getDateInt();
        String address = mainLayoutGroup.getAddress();
        List<Long> tagIds = mainLayoutGroup.getTagIds();
        Long mainLayoutGroupId = mainLayoutGroup.getId();
        PostingType announcementType = PostingType.ANNOUNCEMENT;
        List<LayoutPostMapping> newLayoutPosts = new ArrayList<>();
        boolean timeLimited = dateType != DateType.NONE && dateInt != null && dateInt > 0;
        boolean hasTags = tagIds != null && !tagIds.isEmpty();
        boolean hasAddress = address != null && !address.isEmpty();
        List<Long> postIds = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        if (hasTags) {
            List<Long> postIdsByTags = tagMappingRepository.findPostIdsByTagIds(tagIds);
            if (!timeLimited && !hasAddress) {
                postIds = switch (sortType) {
                    case TOTAL_ENGAGEMENT ->
                            postDetailInfoRepository.findAllPostIdsOrderByTotalEngagementDescInPostIds(
                                    postIdsByTags, announcementType);
                    case VIEW_COUNT ->
                            postDetailInfoRepository.findAllPostIdsOrderByViewCountDescInPostIds(
                                    postIdsByTags, announcementType);
                    case LIKE_COUNT ->
                            postDetailInfoRepository.findAllPostIdsOrderByLikeCountDescInPostIds(
                                    postIdsByTags, announcementType);
                    case PARTICIPANT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsOrderByParticipantCountDescInPostIds(
                                    postIdsByTags, announcementType);
                    case LIKE_COMMENT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsOrderByLikeCommentCountDescInPostIds(
                                    postIdsByTags, announcementType);
                };
            }
            else if (timeLimited && !hasAddress) {
                LocalDateTime fromDate = getFromDate(dateType, dateInt, now);
                postIds = switch (sortType) {
                    case TOTAL_ENGAGEMENT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByTotalEngagementDescInPostIds(
                                    fromDate, postIdsByTags, announcementType);
                    case VIEW_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByViewCountDescInPostIds(
                                    fromDate, postIdsByTags, announcementType);
                    case LIKE_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByLikeCountDescInPostIds(
                                    fromDate, postIdsByTags, announcementType);
                    case PARTICIPANT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByParticipantCountDescInPostIds(
                                    fromDate, postIdsByTags, announcementType);
                    case LIKE_COMMENT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByLikeCommentCountDescInPostIds(
                                    fromDate, postIdsByTags, announcementType);
                };
            }
            else if (!timeLimited) {
                postIds = switch (sortType) {
                    case TOTAL_ENGAGEMENT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByTotalEngagementDescInPostIds(
                                    address, postIdsByTags, announcementType);
                    case VIEW_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByViewCountDescInPostIds(
                                    address, postIdsByTags, announcementType);
                    case LIKE_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByLikeCountDescInPostIds(
                                    address, postIdsByTags, announcementType);
                    case PARTICIPANT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByParticipantCountDescInPostIds(
                                    address, postIdsByTags, announcementType);
                    case LIKE_COMMENT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByLikeCommentCountDescInPostIds(
                                    address, postIdsByTags, announcementType);
                };
            }
            else {
                LocalDateTime fromDate = getFromDate(dateType, dateInt, now);
                postIds = switch (sortType) {
                    case TOTAL_ENGAGEMENT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByTotalEngagementDescInPostIds(
                                    address, fromDate, postIdsByTags, announcementType);
                    case VIEW_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByViewCountDescInPostIds(
                                    address, fromDate, postIdsByTags, announcementType);
                    case LIKE_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByLikeCountDescInPostIds(
                                    address, fromDate, postIdsByTags, announcementType);
                    case PARTICIPANT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByParticipantCountDescInPostIds(
                                    address, fromDate, postIdsByTags, announcementType);
                    case LIKE_COMMENT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByLikeCommentCountDescInPostIds(
                                    address, fromDate, postIdsByTags, announcementType);
                };
            }
        }
        else {
            if (!timeLimited && !hasAddress) {
                postIds = switch (sortType) {
                    case TOTAL_ENGAGEMENT ->
                            postDetailInfoRepository.findAllPostIdsOrderByTotalEngagementDesc(announcementType);
                    case VIEW_COUNT ->
                            postDetailInfoRepository.findAllPostIdsOrderByViewCountDesc(announcementType);
                    case LIKE_COUNT ->
                            postDetailInfoRepository.findAllPostIdsOrderByLikeCountDesc( announcementType);
                    case PARTICIPANT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsOrderByParticipantCountDesc(announcementType);
                    case LIKE_COMMENT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsOrderByLikeCommentCountDesc(announcementType);
                };
            }
            else if (timeLimited && !hasAddress) {

                LocalDateTime fromDate = getFromDate(dateType, dateInt, now);
                postIds = switch (sortType) {
                    case TOTAL_ENGAGEMENT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByTotalEngagementDesc(
                                    fromDate, announcementType);
                    case VIEW_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByViewCountDesc(
                                    fromDate, announcementType);
                    case LIKE_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByLikeCountDesc(
                                    fromDate, announcementType);
                    case PARTICIPANT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByParticipantCountDesc(
                                    fromDate, announcementType);
                    case LIKE_COMMENT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByCreatedAtAfterOrderByLikeCommentCountDesc(
                                    fromDate, announcementType);
                };
            }
            else if (!timeLimited) {
                postIds = switch (sortType) {
                    case TOTAL_ENGAGEMENT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByTotalEngagementDesc(
                                    address, announcementType);
                    case VIEW_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByViewCountDesc(
                                    address, announcementType);
                    case LIKE_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByLikeCountDesc(
                                    address, announcementType);
                    case PARTICIPANT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByParticipantCountDesc(
                                    address, announcementType);
                    case LIKE_COMMENT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressOrderByLikeCommentCountDesc(
                                    address, announcementType);
                };
            }
            else {
                LocalDateTime fromDate = getFromDate(dateType, dateInt, now);
                postIds = switch (sortType) {
                    case TOTAL_ENGAGEMENT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByTotalEngagementDesc(
                                    address, fromDate, announcementType);
                    case VIEW_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByViewCountDesc(
                                    address, fromDate, announcementType);
                    case LIKE_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByLikeCountDesc(
                                    address, fromDate, announcementType);
                    case PARTICIPANT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByParticipantCountDesc(
                                    address, fromDate, announcementType);
                    case LIKE_COMMENT_COUNT ->
                            postDetailInfoRepository.findAllPostIdsByAddressCreatedAtAfterOrderByLikeCommentCountDesc(
                                    address, fromDate, announcementType);
                };
            }
        }
        log.warn(
                "Invalid MainLayoutGroup configuration for code {}: timeLimited={}, hasTags={}, hasAddress={}",
                mainLayoutGroupId, timeLimited, hasTags, hasAddress);
        for (Long postId : postIds) {
            LayoutPostMapping mapping = new LayoutPostMapping(mainLayoutGroup, postId);
            newLayoutPosts.add(mapping);
        }
        if(mainLayoutGroupId !=null)layoutPostMappingRepository.deleteAllByMainLayoutGroup(mainLayoutGroup);
        layoutPostMappingRepository.saveAll(newLayoutPosts);
    }

    private LocalDateTime getFromDate(DateType dateType, Integer dateInt, LocalDateTime now) {
        return switch (dateType) {
            case DAY -> now.minusDays(dateInt);
            case WEEK -> now.minusWeeks(dateInt);
            case MONTH -> now.minusMonths(dateInt);
            case YEAR -> now.minusYears(dateInt);
            default -> LocalDateTime.now()
                    .minusYears(10);
        };
    }

    @Transactional
    public void updateDailyLayoutPosts() {

        List<MainLayoutGroup> scheduledLayouts =
                mainLayoutRepository.findByScheduleType(ScheduleType.DAILY);
        for (MainLayoutGroup layout : scheduledLayouts) {
            setLayoutPosts(layout);
        }
    }

    @Transactional
    public void updateWeeklyLayoutPosts() {
        List<MainLayoutGroup> scheduledLayouts =
                mainLayoutRepository.findByScheduleType(ScheduleType.WEEKLY);
        for (MainLayoutGroup layout : scheduledLayouts) {
            setLayoutPosts(layout);
        }
    }

    @Transactional
    public void updateMonthlyLayoutPosts() {
        List<MainLayoutGroup> scheduledLayouts =
                mainLayoutRepository.findByScheduleType(ScheduleType.MONTHLY);
        for (MainLayoutGroup layout : scheduledLayouts) {
            setLayoutPosts(layout);
        }
    }

    @Transactional
    public void updateYearlyLayoutPosts() {
        List<MainLayoutGroup> scheduledLayouts =
                mainLayoutRepository.findByScheduleType(ScheduleType.YEARLY);
        for (MainLayoutGroup layout : scheduledLayouts) {
            setLayoutPosts(layout);
        }
    }

    @Transactional(readOnly = true)
    public List<PostDto.PostTitleItem> getMainLayoutPosts(Long id) {
        MainLayoutGroup layout = mainLayoutRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExceptionCode.LAYOUT_NOT_FOUND));
        List<Long> postIds = layoutPostMappingRepository.findPostIdsByMainLayoutGroup(layout);
        List<PostDto.PostTitleItem> posts = new ArrayList<>();
        if (!postIds.isEmpty()) {
            List<RedisDto.PostDto> postDtos = cacheService.getMultiplePostsFromCache(postIds);
            List<Long> userIds = postDtos.stream()
                    .map(RedisDto.PostDto::getUserId)
                    .collect(Collectors.toList());
            List<RedisDto.UserDto> userMap = cacheService.getMultipleUsersFromCache(userIds);
            Map<Long, RedisDto.UserDto> userDtoMap = new HashMap<>();
            for (RedisDto.UserDto userDto : userMap) {
                userDtoMap.put(userDto.getUserId(), userDto);
            }
            List<TagMapping> tags = tagMappingRepository.findByPostIdInIncludingAdmin(postIds);
            Map<Long, List<Tag>> tagMap = new HashMap<>();
            for (TagMapping tagMapping : tags) {
                Long postId = tagMapping.getPosting()
                        .getId();
                tagMap.computeIfAbsent(postId, k -> new ArrayList<>())
                        .add(tagMapping.getTag());
            }
            // postIds 순서에 맞게 정렬
            for (Long postId : postIds) {
                for (RedisDto.PostDto postDto : postDtos) {
                    if (postDto.getPostId()
                            .equals(postId)) {
                        posts.add(new PostDto.PostTitleItem(postDto,
                                userDtoMap.get(postDto.getUserId()),
                                tagMap.getOrDefault(postId, new ArrayList<>())));
                        break;
                    }
                }
            }
        }
        return posts;
    }

    public boolean deleteMainLayoutPosts(Long mainLayoutId, Long postId) {
        MainLayoutGroup layout = mainLayoutRepository.findById(mainLayoutId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.LAYOUT_NOT_FOUND));
        if (!layoutPostMappingRepository.existsByMainLayoutGroupAndPostId(layout, postId)) {
            return false;
        }
        layoutPostMappingRepository.deleteByMainLayoutGroupAndPostId(layout, postId);
        return true;
    }
}
