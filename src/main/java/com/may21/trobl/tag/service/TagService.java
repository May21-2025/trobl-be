package com.may21.trobl.tag.service;

import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.dto.TagDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TagService {
    Set<Tag> createTags(List<TagDto.Request> tagRequests);

    List<TagMapping> createTagMapping(Set<Tag> tags, Posting post);

    List<TagDto.Response> getStaticTags();

    List<TagMapping> updateTags(Set<Tag> tags, Posting post);

    List<Tag> getPostTags(Posting post);

    List<TagDto.Response> searchTags(String keyword);

    boolean organize();

    Map<Long, List<Tag>> getPostTagsMap(List<Posting> postList);
}
