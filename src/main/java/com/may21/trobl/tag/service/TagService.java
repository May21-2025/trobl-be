package com.may21.trobl.tag.service;

import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.dto.TagDto;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TagService {
    Set<Tag> createTags(List<TagDto.Request> tagRequests);

    List<TagMapping> createTagMapping(Set<Tag> tags, Posting post);

    List<TagDto.Response> getStaticTags();

    List<TagMapping> updateTags(Set<Tag> tags, Posting post);

    List<Tag> getPostTags(Long postId);

    List<TagDto.Response> searchTags(String keyword);

    boolean organize();

    Map<Long, List<Tag>> getPostTagsMap(List<Posting> postList);

    Map<Long, Tag> getLayoutTagMap(Set<Long> tagIds, Map<Long, List<Long>> tagIdMap);

    List<Tag> getTagsByIds(HashSet<Long> longs);

    @Transactional(readOnly = true)
    Page<TagDto.TagPoolDto> getTagPools(int page, int size, String sortBy);

    @Transactional(readOnly = true)
    List<TagDto.TagInfo> getTagsInfoByTagPoolId(Long tagPoolId);

    @Transactional
    TagDto.TagInfo createTag(String tagName, Long tagPoolId);

    @Transactional
    TagDto.TagInfo updateTagPoolOfTag(Long tagId, Long tagPoolId);

    @Transactional
    TagDto.TagPoolDto createTagPool(String tagPoolName);

    @Transactional
    boolean deleteTagPool(Long tagPoolId);
}
