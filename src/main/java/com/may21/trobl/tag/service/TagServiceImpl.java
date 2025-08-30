package com.may21.trobl.tag.service;

import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.utility.ProfanityFilter;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.domain.TagPool;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.repository.TagMappingRepository;
import com.may21.trobl.tag.repository.TagPoolRepository;
import com.may21.trobl.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;
    private final ProfanityFilter profanityFilter;
    private final TagPoolRepository tagPoolRepository;

    @Override
    public Set<Tag> createTags(List<TagDto.Request> tagRequests) {
        if (tagRequests == null || tagRequests.isEmpty()) {
            return new HashSet<>();
        }
        List<Long> existingTagIds = new ArrayList<>();
        List<String> tagNames = new ArrayList<>();
        List<TagDto.Request> newTagRequests = new ArrayList<>();
        for (TagDto.Request tagRequest : tagRequests) {
            Long tagId = tagRequest.getTagId();
            if (tagId != null) {
                existingTagIds.add(tagId);
            }
            else if (!profanityFilter.containsProfanity(tagRequest.getName())) {
                newTagRequests.add(tagRequest);
                tagNames.add(tagRequest.getName());
            }
        }
        List<Tag> existingTags = tagRepository.findAllByIdsAndNames(existingTagIds, tagNames);
        List<String> existingTagNames = existingTags.stream()
                .map(Tag::getName)
                .toList();
        List<Tag> newTags = new ArrayList<>();
        for (TagDto.Request tagRequest : newTagRequests) {
            if (existingTagNames.contains(tagRequest.getName())) continue;
            Tag tag = new Tag(tagRequest.getName());
            newTags.add(tag);
        }
        tagRepository.saveAll(newTags);
        Set<Tag> allTags = new HashSet<>(existingTags);
        allTags.addAll(newTags);
        allTags.addAll(existingTags);
        return allTags;
    }

    @Override
    public List<TagMapping> createTagMapping(Set<Tag> tags, Posting post) {
        List<TagMapping> tagMappings = new ArrayList<>();
        for (Tag tag : tags) {
            TagMapping tagMapping = new TagMapping(tag, post);
            tagMappings.add(tagMapping);
        }
        return tagMappings;
    }

    @Override
    public List<TagDto.Response> getStaticTags() {
        List<Tag> tags = tagRepository.findStaticTags();
        return TagDto.Response.fromTagList(tags);
    }

    @Override
    public List<TagMapping> updateTags(Set<Tag> tags, Posting post) {
        List<TagMapping> tagMappings = new ArrayList<>();
        for (Tag tag : tags) {
            TagMapping tagMapping = new TagMapping(tag, post);
            tagMappings.add(tagMapping);
        }
        // 기존 태그 매핑을 삭제하고 새로 추가
        post.getTags()
                .clear();
        post.getTags()
                .addAll(tagMappings);
        // 따라서 여기서는 단순히 매핑 리스트를 반환
        return tagMappingRepository.saveAll(tagMappings);
    }

    @Override
    public List<Tag> getPostTags(Long postId) {

        return tagMappingRepository.getTagsByPostIdAndNotAdmin(postId);
    }

    @Override
    public List<TagDto.Response> searchTags(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            List<Tag> tags = tagRepository.findDistinctTagsByNameContaining(keyword);
            return TagDto.Response.fromTagList(tags);
        }
        return List.of();
    }

    @Override
    public boolean organize() {
        // 같은 이름을 가진 Tag가 있을 경우 그중 하나만 남기고 다 삭제한다.
        // 삭제되는 Tag가 TagMapping에 연결되어 있다면, 해당 TagMapping에 남은 하나의 Tag로 연결한다.
        List<Tag> tags = tagRepository.findAll();
        Map<String, Tag> uniqueTagNames = new HashMap<>();
        List<Tag> tagsToDelete = new ArrayList<>();
        for (Tag tag : tags) {
            if (uniqueTagNames.containsKey(tag.getName())) {
                tagsToDelete.add(tag);
            }
            else {
                uniqueTagNames.put(tag.getName(), tag);
            }
        }
        if (tagsToDelete.isEmpty()) {
            return true;
        }
        List<TagMapping> onesToSave = new ArrayList<>();
        for (Tag tagToDelete : tagsToDelete) {
            List<TagMapping> tagMappings =
                    tagMappingRepository.findByTagAndAdminIsFalseOrAdminIsNull(tagToDelete);
            if (!tagMappings.isEmpty()) {
                Tag remainingTag = uniqueTagNames.get(tagToDelete.getName());
                for (TagMapping tagMapping : tagMappings) {
                    tagMapping.setTag(remainingTag);
                }
                onesToSave.addAll(tagMappings);
            }
        }
        tagMappingRepository.saveAll(onesToSave);
        tagRepository.deleteAll(tagsToDelete);
        return false;
    }

    @Override
    public Map<Long, List<Tag>> getPostTagsMap(List<Posting> postList) {
        if (postList == null || postList.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Tag>> postTagsMap = new HashMap<>();
        List<TagMapping> tagMappings =
                tagMappingRepository.findByPostingInAndAdminIsFalseOrAdminIsNull(postList);
        for (TagMapping tagMapping : tagMappings) {
            Long postId = tagMapping.getPosting()
                    .getId();
            postTagsMap.computeIfAbsent(postId, k -> new ArrayList<>())
                    .add(tagMapping.getTag());
        }
        return postTagsMap;
    }

    @Override
    public Map<Long, Tag> getLayoutTagMap(Set<Long> tagIds, Map<Long, List<Long>> tagIdMap) {
        if (tagIds != null && !tagIds.isEmpty() && tagIdMap != null && !tagIdMap.isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(tagIds);
            Map<Long, Tag> tagMap = new HashMap<>();
            for (Tag tag : tags) {
                tagMap.put(tag.getId(), tag);
            }
            return tagMap;
        }
        return Map.of();
    }

    @Override
    public List<Tag> getTagsByIds(HashSet<Long> longs) {
        if (longs != null && !longs.isEmpty()) {
            return tagRepository.findAllById(longs);
        }
        return List.of();
    }

    @Override
    public Page<TagDto.TagPoolDto> getTagPools(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<TagPool> tagPools = tagPoolRepository.findAll(pageable);
        return tagPools.map(TagDto.TagPoolDto::new);
    }

    @Override
    public List<TagDto.TagInfo> getTagsInfoByTagPoolId(Long tagPoolId) {
        List<Tag> tags = tagPoolId == null ? tagRepository.findByTagPoolIsNull() :
                tagRepository.findByTagPoolId(tagPoolId);
        TagPool tagPool = tagPoolId == null ? null : tagPoolRepository.findById(tagPoolId)
                .orElse(null);
        return TagDto.TagInfo.fromTags(tags, tagPool);
    }

    @Override
    public TagDto.TagInfo createTag(String tagName, Long tagPoolId) {
        if (tagRepository.existsByName(tagName)) {
            throw new BusinessException(ExceptionCode.TAG_EXISTS);
        }
        if (profanityFilter.containsProfanity(tagName)) {
            throw new BusinessException(ExceptionCode.TAG_PROFANITY);
        }
        TagPool tagPool = tagPoolId != null ? tagPoolRepository.findById(tagPoolId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TAG_POOL_NOT_FOUND)) : null;
        Tag tag = new Tag(tagName, tagPool);
        tagRepository.save(tag);
        return new TagDto.TagInfo(tag);
    }

    @Override
    public TagDto.TagInfo updateTagPoolOfTag(Long tagId, Long tagPoolId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TAG_NOT_FOUND));
        if (tagPoolId == null) {
            tag.setTagPool(null);
            return new TagDto.TagInfo(tag);
        }
        if (tag.getTagPool() != null && tag.getTagPool()
                .getId()
                .equals(tagPoolId)) {return new TagDto.TagInfo(tag);}
        TagPool tagPool = tagPoolRepository.findById(tagPoolId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TAG_POOL_NOT_FOUND));
        tag.setTagPool(tagPool);
        return new TagDto.TagInfo(tag);
    }

    @Override
    public TagDto.TagPoolDto createTagPool(String tagPoolName) {
        if (tagPoolRepository.existsByName(tagPoolName)) {
            throw new BusinessException(ExceptionCode.TAG_POOL_EXISTS);
        }
        TagPool tagPool = new TagPool(tagPoolName);
        tagPoolRepository.save(tagPool);
        return new TagDto.TagPoolDto(tagPool);
    }

    @Override
    public boolean deleteTagPool(Long tagPoolId) {
        TagPool tagPool = tagPoolRepository.findById(tagPoolId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TAG_POOL_NOT_FOUND));
        List<Tag> tags = tagRepository.findByTagPoolId(tagPoolId);
        if (!tags.isEmpty()) {
            for(Tag tag : tags) {
                tag.setTagPool(null);
            }
        }
        tagPoolRepository.delete(tagPool);
        return true;
    }

}
