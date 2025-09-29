package com.may21.trobl.tag.service;

import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.dto.TagDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Transactional
public interface TagService {
    Set<Tag> createTags(List<TagDto.Request> tagRequests);
    List<TagMapping> createTagMapping(Set<Tag> tags, Posting post);

    List<TagMapping> updateTags(Set<Tag> tags, Posting post);

    @Transactional(readOnly = true)
    List<Tag> getPostTags(Long postId);

    // 기존 메서드 (하위 호환성)
    @Transactional(readOnly = true)
    List<TagDto.Response> searchTags(String keyword);

    // 페이징 지원 메서드들
    @Transactional(readOnly = true)
    Page<TagDto.Response> searchTags(String keyword, Pageable pageable);

    @Transactional(readOnly = true)
    Page<TagDto.Response> getAllTags(Pageable pageable);

    @Transactional(readOnly = true)
    Page<TagDto.TagInfo> getTagsInfoByTagPoolId(Long tagPoolId, Pageable pageable);

    boolean organize();

    @Transactional(readOnly = true)
    Map<Long, List<Tag>> getPostTagsMap(List<Posting> postList);

    @Transactional(readOnly = true)
    Map<Long, Tag> getLayoutTagMap(Set<Long> tagIds, Map<Long, List<Long>> tagIdMap);

    @Transactional(readOnly = true)
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

    // 배치 처리 메서드
    @Transactional
    void updateTagsBatch(List<Long> tagIds, Long tagPoolId);
    
    // 키워드 관련 메서드들
    @Transactional
    Long addKeywordToTag(Long tagId, String keyword);
    
    @Transactional
    void removeKeywordFromTag(Long keywordId);
    
    @Transactional(readOnly = true)
    TagDto.Keywords getKeywordsByTag(Long tagId);
    
    @Transactional(readOnly = true)
    List<String> analyzePostContent(String content);

    Map<Tag, Set<String>>  getKeywordMap();
    
    @Transactional(readOnly = true)
    List<Long> getTagsWithKeywords();
}
