package com.may21.trobl.tag.service;

import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.repository.TagMappingRepository;
import com.may21.trobl.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;

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
            } else {
                newTagRequests.add(tagRequest);
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
        post.getTags().clear();
        post.getTags().addAll(tagMappings);
        // 따라서 여기서는 단순히 매핑 리스트를 반환
        return tagMappingRepository.saveAll(tagMappings);
    }

    @Override
    public List<Tag> getPostTags(Long postId) {

        return tagMappingRepository.getTagsByPostId(postId);
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
            } else {
                uniqueTagNames.put(tag.getName(), tag);
            }
        }
        if (tagsToDelete.isEmpty()) {
            return true;
        }
        List<TagMapping> onesToSave = new ArrayList<>();
        for (Tag tagToDelete : tagsToDelete) {
            List<TagMapping> tagMappings = tagMappingRepository.findByTag(tagToDelete);
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
        List<TagMapping> tagMappings = tagMappingRepository.findByPostingIn(postList);
        for (TagMapping tagMapping : tagMappings) {
            Long postId = tagMapping.getPosting().getId();
            postTagsMap.computeIfAbsent(postId, k -> new ArrayList<>()).add(tagMapping.getTag());
        }
        return postTagsMap;
    }

}
