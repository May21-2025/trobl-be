package com.may21.trobl.tag.service;

import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.repository.TagMappingRepository;
import com.may21.trobl.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;

    @Override
    public Set<Tag> createTags(List<TagDto.Request> tagRequests) {
        List<Long>  existingTagIds = new ArrayList<>();
        List<String> tagNames = new ArrayList<>();
        List<TagDto.Request> newTagRequests = new ArrayList<>();
        for (TagDto.Request tagRequest : tagRequests) {
            Long tagId = tagRequest.getTagId();
            if(tagId != null) {
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
            if(existingTagNames.contains(tagRequest.getName())) continue;
            Tag tag = new Tag(tagRequest.getName(), tagRequest.getColor());
            newTags.add(tag);
        }
        tagRepository.saveAll(newTags);
        Set<Tag> allTags = new HashSet<>(existingTags);
        allTags.addAll(newTags);
        allTags.addAll(existingTags);
        return allTags;
    }

    @Override
    public List<TagMapping> createTagMapping(Set<Tag> tags, Posting post){
        List<TagMapping> tagMappings = new ArrayList<>();
        for (Tag tag : tags) {
            TagMapping tagMapping = new TagMapping(tag, post);
            tagMappings.add(tagMapping);
        }
        return tagMappings;
    }

    @Override
    public List<TagDto.Response> getStaticTags(){
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
    public List<Tag> getPostTags(Posting post) {

        return tagMappingRepository.getTagsByPost(post);
    }

}
