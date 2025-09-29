package com.may21.trobl.admin.service;

import com.may21.trobl._global.utility.PostExamineTagValue;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagKeyword;
import com.may21.trobl.tag.domain.TagPool;
import com.may21.trobl.tag.repository.TagKeywordRepository;
import com.may21.trobl.tag.repository.TagPoolRepository;
import com.may21.trobl.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TagMigrationService {

    private final TagPoolRepository tagPoolRepository;
    private final TagRepository tagRepository;
    private final TagKeywordRepository tagKeywordRepository;

    // 기존 PostExamineTagValue의 데이터를 DB로 마이그레이션
    public void migrateExistingData() {
        createTagsAndKeywords();
    }

    @Transactional
    public void createTagsAndKeywords() {
        Map<String, TagPool> tagPools = tagPoolRepository.findAll().stream()
                .collect(Collectors.toMap(TagPool::getName, Function.identity()));

        Set<String> allTagNames = PostExamineTagValue.TAG_POOL.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        // 모든 태그와 키워드를 미리 조회
        List<Tag> existingTagEntities = tagRepository.findByNameIn(allTagNames);
        Map<String, Tag> existingTagMap = existingTagEntities.stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));

        // 태그별로 현재 저장된 키워드 조회
        Map<Long, Set<String>> existingKeywordsByTagId =
                tagKeywordRepository.findByTagIn(existingTagEntities).stream()
                        .collect(Collectors.groupingBy(
                                tk -> tk.getTag().getId(),
                                Collectors.mapping(TagKeyword::getKeyword, Collectors.toSet())
                        ));

        List<Tag> newTags = new ArrayList<>();
        List<TagKeyword> newKeywords = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : PostExamineTagValue.TAG_POOL.entrySet()) {
            String poolName = entry.getKey();
            List<String> tagNames = entry.getValue();

            TagPool tagPool = Optional.ofNullable(tagPools.get(poolName))
                    .orElseThrow(() -> new RuntimeException("TagPool not found: " + poolName));

            for (String tagName : tagNames) {
                Tag tag = existingTagMap.get(tagName);

                // 태그가 없으면 새로 생성
                if (tag == null) {
                    tag = new Tag(tagName, tagPool);
                    newTags.add(tag);
                    existingTagMap.put(tagName, tag);
                }

                // 키워드 처리
                Set<String> keywords = PostExamineTagValue.KEYWORD_MAPPINGS.getOrDefault(tagName, Set.of());
                Set<String> existingKeywords = existingKeywordsByTagId
                        .getOrDefault(tag.getId(), Set.of());

                for (String keyword : keywords) {
                    if (!existingKeywords.contains(keyword)) {
                        newKeywords.add(new TagKeyword(tag, keyword));
                    }
                }
            }
        }

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
        }
        if (!newKeywords.isEmpty()) {
            tagKeywordRepository.saveAll(newKeywords);
        }
    }



    // 특정 TagPool의 데이터만 마이그레이션
    public void migrateTagPoolData(String poolName) {
        List<String> tagNames = PostExamineTagValue.TAG_POOL.get(poolName);
        if (tagNames == null) {
            throw new RuntimeException("TagPool not found in static data: " + poolName);
        }
        
        TagPool tagPool = tagPoolRepository.findByName(poolName)
                .orElseThrow(() -> new RuntimeException("TagPool not found in DB: " + poolName));
        
        for (String tagName : tagNames) {
            if (!tagRepository.existsByName(tagName)) {
                Tag tag = new Tag(tagName, tagPool);
                tag = tagRepository.save(tag);
                
                Set<String> keywords = PostExamineTagValue.KEYWORD_MAPPINGS.get(tagName);
                if (keywords != null) {
                    for (String keyword : keywords) {
                        TagKeyword tagKeyword = new TagKeyword(tag, keyword);
                        tagKeywordRepository.save(tagKeyword);
                    }
                }
            }
        }
    }
}

