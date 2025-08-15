package com.may21.trobl._global.component;

import com.may21.trobl._global.utility.PostExamineTagValue;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagPool;
import com.may21.trobl.tag.repository.TagPoolRepository;
import com.may21.trobl.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TagInitializer implements CommandLineRunner {

    private final TagRepository tagRepository;
    private final TagPoolRepository tagPoolRepository;

    @Override
    public void run(String... args) {
        initializeTagPoolsAndTags();
    }

    private void initializeTagPoolsAndTags() {
        log.info("태그 풀과 태그 초기화 작업을 시작합니다.");

        // 1. PostExamineTagValue의 TAG_POOL에서 TagPool 생성
        Map<String, TagPool> tagPoolMap = createTagPools();
        
        // 2. 각 TagPool에 해당하는 Tag들 생성
        createTagsForAllPools(tagPoolMap);

        log.info("태그 풀과 태그 초기화 작업이 완료되었습니다.");
    }

    private Map<String, TagPool> createTagPools() {
        Map<String, TagPool> tagPoolMap = new HashMap<>();
        
        log.info("TagPool 생성을 시작합니다. 총 {}개의 풀이 필요합니다.", PostExamineTagValue.TAG_POOL.size());
        List<TagPool> newTagPool = new ArrayList<>();
        // PostExamineTagValue의 TAG_POOL에서 TagPool 생성
        for (String poolName : PostExamineTagValue.TAG_POOL.keySet()) {
            TagPool tagPool = tagPoolRepository.findByName(poolName)
                    .orElseGet(() -> {
                        log.info("새로운 TagPool '{}'을 생성합니다.", poolName);
                        TagPool newPool = new TagPool(poolName);
                        newTagPool.add(newPool);
                        return newPool;
                    });
            tagPoolRepository.saveAll(newTagPool);
            tagPoolMap.put(poolName, tagPool);
            log.info("TagPool '{}' 준비 완료 (ID: {})", poolName, tagPool.getId());
        }
        
        log.info("총 {}개의 TagPool이 준비되었습니다.", tagPoolMap.size());
        return tagPoolMap;
    }

    private void createTagsForAllPools(Map<String, TagPool> tagPoolMap) {
        int totalCreatedTags = 0;
        
        log.info("각 TagPool에 태그 생성을 시작합니다.");
        
        // 각 TagPool에 해당하는 Tag들 생성
        for (Map.Entry<String, List<String>> entry : PostExamineTagValue.TAG_POOL.entrySet()) {
            String poolName = entry.getKey();
            List<String> tagNames = entry.getValue();
            TagPool tagPool = tagPoolMap.get(poolName);
            
            if (tagPool == null) {
                log.warn("TagPool '{}'을 찾을 수 없습니다.", poolName);
                continue;
            }
            
            log.info("TagPool '{}'에 {}개의 태그를 생성합니다.", poolName, tagNames.size());
            int createdTags = createTagsForPool(tagPool, tagNames);
            totalCreatedTags += createdTags;
            
            log.info("TagPool '{}'에 {}개의 새로운 태그가 생성되었습니다.", poolName, createdTags);
        }
        
        log.info("총 {}개의 새로운 태그가 생성되었습니다.", totalCreatedTags);
        log.info("현재 전체 태그 개수: {}", tagRepository.count());
    }

    private int createTagsForPool(TagPool tagPool, List<String> tagNames) {
        // 현재 데이터베이스에 있는 태그들의 이름을 가져옴 (해당 풀에 속한 것들만)
        Set<String> existingTagNames = tagRepository.findAll()
                .stream()
                .filter(tag -> tag.getTagPool() != null && tag.getTagPool().getId().equals(tagPool.getId()))
                .map(Tag::getName)
                .collect(Collectors.toSet());

        log.debug("TagPool '{}'에 이미 존재하는 태그: {}개", tagPool.getName(), existingTagNames.size());

        // 없는 태그들만 필터링하여 새로 생성
        List<Tag> newTags = tagNames.stream()
                .filter(tagName -> !existingTagNames.contains(tagName))
                .map(tagName -> new Tag(tagName, tagPool))
                .collect(Collectors.toList());

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
            log.info("TagPool '{}'에 {}개의 새로운 태그가 생성되었습니다.", 
                    tagPool.getName(), newTags.size());
            
            // 생성된 태그들 로깅
            newTags.forEach(tag -> log.debug("생성된 태그: {} (풀: {})", 
                    tag.getName(), tagPool.getName()));
        } else {
            log.info("TagPool '{}'에 새로 생성할 태그가 없습니다.", tagPool.getName());
        }

        return newTags.size();
    }
}