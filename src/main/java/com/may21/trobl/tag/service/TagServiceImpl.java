package com.may21.trobl.tag.service;

import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl._global.utility.ProfanityFilter;
import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagKeyword;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.domain.TagPool;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.repository.TagKeywordRepository;
import com.may21.trobl.tag.repository.TagMappingRepository;
import com.may21.trobl.tag.repository.TagPoolRepository;
import com.may21.trobl.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMappingRepository tagMappingRepository;
    private final ProfanityFilter profanityFilter;
    private final TagPoolRepository tagPoolRepository;
    private final TagKeywordRepository tagKeywordRepository;

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
    @Transactional(readOnly = true)
    public List<Tag> getPostTags(Long postId) {
        // 페치 조인으로 N+1 문제 해결
        List<TagMapping> tagMappings = tagMappingRepository.findByPostIdWithTag(postId);
        return tagMappings.stream()
                .map(TagMapping::getTag)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagDto.Response> searchTags(String keyword) {
        if (StringUtils.isNotBlank(keyword)) {
            List<Tag> tags = tagRepository.findDistinctTagsByNameContaining(keyword);
            return TagDto.Response.fromTagList(tags);
        }
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TagDto.Response> searchTags(String keyword, Pageable pageable) {
        if (StringUtils.isBlank(keyword)) {
            return Page.empty(pageable);
        }

        Page<Tag> tags = tagRepository.findDistinctTagsByNameContainingPaged(keyword, pageable);
        return tags.map(tag -> new TagDto.Response(tag.getId(), tag.getName()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TagDto.Response> getAllTags(Pageable pageable) {
        Page<Tag> tags = tagRepository.findAllTags(pageable);
        return tags.map(tag -> new TagDto.Response(tag.getId(), tag.getName()));
    }


    @Override
    @Transactional(readOnly = true)
    public Page<TagDto.TagInfo> getTagsInfoByTagPoolId(Long tagPoolId, Pageable pageable) {
        Page<Tag> tags = tagPoolId == null ? tagRepository.findByTagPoolIsNull(pageable) :
                tagRepository.findByTagPoolId(tagPoolId, pageable);

        return tags.map(tag -> new TagDto.TagInfo(tag));
    }

    @Override
    @Transactional
    public boolean organize() {
        log.info("태그 정리 작업 시작");

        // 배치 처리로 메모리 효율성 개선
        int batchSize = 1000;
        int offset = 0;
        boolean hasChanges = false;

        while (true) {
            List<Tag> tags = tagRepository.findTagsWithPagination(offset, batchSize);
            if (tags.isEmpty()) break;

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

            if (!tagsToDelete.isEmpty()) {
                updateTagMappingsAndDeleteTags(tagsToDelete, uniqueTagNames);
                hasChanges = true;
            }

            offset += batchSize;
        }

        log.info("태그 정리 작업 완료. 변경사항: {}", hasChanges);
        return hasChanges;
    }

    private void updateTagMappingsAndDeleteTags(List<Tag> tagsToDelete,
            Map<String, Tag> uniqueTagNames) {
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

        if (!onesToSave.isEmpty()) {
            tagMappingRepository.saveAll(onesToSave);
        }
        tagRepository.deleteAll(tagsToDelete);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Tag>> getPostTagsMap(List<Posting> postList) {
        if (postList == null || postList.isEmpty()) {
            return Map.of();
        }

        // 1. Post ID만 추출
        List<Long> postIds = postList.stream()
                .map(Posting::getId)
                .collect(Collectors.toList());

        // 2. 페치 조인으로 한 번에 조회 (N+1 문제 해결)
        List<TagMapping> tagMappings = tagMappingRepository.findByPostIdInWithTag(postIds);

        // 3. Map으로 그룹화
        return tagMappings.stream()
                .collect(Collectors.groupingBy(tm -> tm.getPosting()
                        .getId(), Collectors.mapping(TagMapping::getTag, Collectors.toList())));
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
        if (tagPoolId == -1) {
            tag.setTagPool(null);
            tagRepository.save(tag); // 변경사항 저장
            return new TagDto.TagInfo(tag);
        }
        if (tag.getTagPool() != null && tag.getTagPool()
                .getId()
                .equals(tagPoolId)) {return new TagDto.TagInfo(tag);}
        TagPool tagPool = tagPoolRepository.findById(tagPoolId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TAG_POOL_NOT_FOUND));
        tag.setTagPool(tagPool);
        tagRepository.save(tag); // 변경사항 저장
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
    @Transactional
    public boolean deleteTagPool(Long tagPoolId) {
        TagPool tagPool = tagPoolRepository.findById(tagPoolId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TAG_POOL_NOT_FOUND));

        // 배치 업데이트로 연관된 태그들의 tagPool을 null로 설정
        tagRepository.clearTagPoolFromTags(tagPoolId);

        tagPoolRepository.delete(tagPool);
        return true;
    }

    @Override
    @Transactional
    public void updateTagsBatch(List<Long> tagIds, Long tagPoolId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }

        TagPool tagPool = tagPoolId != null ? tagPoolRepository.findById(tagPoolId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TAG_POOL_NOT_FOUND)) : null;

        // 배치 업데이트
        int updatedCount = tagRepository.updateTagPoolBatch(tagIds, tagPool);
        log.info("{}개의 태그 풀이 업데이트되었습니다.", updatedCount);
    }

    @Override
    public Long addKeywordToTag(Long tagId, String keyword) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.TAG_NOT_FOUND));

        if (keyword == null || keyword.trim()
                .isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }

        String trimmedKeyword = keyword.trim();

        // 중복 키워드 확인
        if (tagKeywordRepository.existsByTagIdAndKeyword(tagId, trimmedKeyword)) {
            throw new BusinessException(ExceptionCode.KEYWORD_ALREADY_EXISTS);
        }

        TagKeyword tagKeyword = new TagKeyword(tag, trimmedKeyword);
        TagKeyword savedKeyword = tagKeywordRepository.save(tagKeyword);
        return savedKeyword.getId();
    }

    @Override
    public void removeKeywordFromTag(Long keywordId) {
        if (!tagKeywordRepository.existsById(keywordId)) {
            throw new BusinessException(ExceptionCode.KEYWORD_NOT_FOUND);
        }

        tagKeywordRepository.deleteById(keywordId);
    }

    @Override
    @Transactional(readOnly = true)
    public TagDto.Keywords getKeywordsByTag(Long tagId) {
        List<TagKeyword> keywords = tagKeywordRepository.findByTagId(tagId);
        return new TagDto.Keywords(keywords.stream()
                .map(TagDto.Keyword::new)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> analyzePostContent(String content) {
        Map<String, Integer> tagScores = new HashMap<>();

        // 모든 태그와 키워드 조회
        List<Tag> allTags = tagRepository.findAll();

        for (Tag tag : allTags) {
            List<TagKeyword> keywords = tagKeywordRepository.findByTagId(tag.getId());

            int score = 0;
            for (TagKeyword tagKeyword : keywords) {
                String keyword = tagKeyword.getKeyword();
                // 키워드가 게시글 내용에 포함되어 있는지 확인 (대소문자 구분 없이)
                if (content.toLowerCase()
                        .contains(keyword.toLowerCase())) {
                    score++;
                }
            }

            if (score > 0) {
                tagScores.put(tag.getName(), score);
            }
        }

        // 점수순으로 정렬하여 상위 태그들 반환
        return tagScores.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue()
                        .reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Tag, Set<String>> getKeywordMap() {
        List<TagKeyword> allKeywords = tagKeywordRepository.findAll();
        Map<Tag, Set<String>> keywordMap = new HashMap<>();
        for (TagKeyword tagKeyword : allKeywords) {
            Tag tag = tagKeyword.getTag();
            String keyword = tagKeyword.getKeyword();
            keywordMap.computeIfAbsent(tag, k -> new HashSet<>())
                    .add(keyword);
        }
        return keywordMap;
    }

}
