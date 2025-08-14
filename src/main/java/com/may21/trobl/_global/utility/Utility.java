package com.may21.trobl._global.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.component.GlobalValues;
import com.may21.trobl._global.enums.PostingType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

import static com.may21.trobl._global.component.GlobalValues.USER_PROFILE_IMAGE_PATH;
import static com.may21.trobl._global.utility.PostExamineTagValue.KEYWORD_MAPPINGS;
import static com.may21.trobl._global.utility.PostExamineTagValue.TAG_POOL;

public class Utility {


    public static String toJson(Map<String, String> data) {
        try {
            return new ObjectMapper().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public static String getRandomString() {
        //make 6개 대소문자 영어와 숫자 4개의 조합으로 랜덤 닉네임 생성
        StringBuilder nickname = new StringBuilder();
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < 5; i++) {
            int randomIndex = (int) (Math.random() * characters.length());
            nickname.append(characters.charAt(randomIndex));
        }
        for (int i = 0; i < 3; i++) {
            int randomIndex = (int) (Math.random() * characters.length());
            nickname.append(characters.charAt(randomIndex));
        }
        return nickname.toString();
    }


    public static Pageable getPageable(int page, int size, String sortBy, String sortDirection) {
        String sort = sortBy != null ? sortBy : "id";
        if (sortDirection.equalsIgnoreCase("asc")) {
            return PageRequest.of(page, size, Sort.by(sort)
                    .ascending());
        }
        else if (sortDirection.equalsIgnoreCase("desc")) {
            return PageRequest.of(page, size, Sort.by(sort)
                    .descending());
        }
        else {
            return Pageable.ofSize(size)
                    .withPage(page);
        }
    }

    public static String getUserProfileUrl(String imageKey) {
        if (imageKey == null || imageKey.isEmpty()) {
            return "";
        }
        return GlobalValues.getCdnUrl() + GlobalValues.getPREFIX() + USER_PROFILE_IMAGE_PATH +
                imageKey;

    }

    public static Pageable getPageable(int size, int page, String sortType, boolean asc) {
        if (sortType == null) {
            return Pageable.ofSize(size)
                    .withPage(page);
        }
        if (asc) {
            return PageRequest.of(page, size, Sort.by(sortType)
                    .ascending());
        }
        else {
            return PageRequest.of(page, size, Sort.by(sortType)
                    .descending());
        }
    }


    public static String stringListToString(List<String> stringList) {
        return stringList == null ? "" : String.join(",", stringList);
    }

    public static int getYearCount(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.isEmpty()) {
            return 0;
        }
        LocalDateTime dateTime;
        if (dateTimeString.length() == 10) {
            dateTime = LocalDate.parse(dateTimeString)
                    .atStartOfDay();
        }
        else {
            dateTime = LocalDateTime.parse(dateTimeString);
        }

        return Period.between(dateTime.toLocalDate(), LocalDate.now())
                .getYears();
    }

    public static List<String> stringtoStringList(String StringListStr) {
        if (StringListStr == null || StringListStr.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(StringListStr.split(","));
    }

    public static List<PostingType> getFilteredPostTypeList(List<String> postTypes) {
        if (postTypes == null || postTypes.isEmpty())
            return List.of(PostingType.POLL, PostingType.GENERAL, PostingType.FAIR_VIEW);
        List<PostingType> postingTypes = new ArrayList<>();
        for (String string : postTypes) {
            PostingType postingType = PostingType.fromString(string);
            if (postingType == null) continue;
            postingTypes.add(postingType);
        }
        return postingTypes;
    }

    /**
     * TAG_POOL에서 특정 키워드를 포함하는 모든 태그를 검색
     *
     * @param keyword 검색할 키워드
     * @return 키워드를 포함하는 태그들의 리스트
     */
    public static List<String> findTagsContainingKeyword(String keyword) {
        List<String> result = new ArrayList<>();

        if (keyword == null || keyword.trim()
                .isEmpty()) {
            return result;
        }

        String lowerKeyword = keyword.toLowerCase()
                .trim();

        // 모든 카테고리의 태그들을 순회하면서 키워드 포함 여부 검사
        for (Map.Entry<String, List<String>> entry : TAG_POOL.entrySet()) {
            List<String> tags = entry.getValue();

            for (String tag : tags) {
                if (tag.toLowerCase()
                        .contains(lowerKeyword)) {
                    result.add(tag);
                }
            }
        }

        // 중복 제거 (혹시 같은 태그가 여러 카테고리에 있을 경우)
        return result.stream()
                .distinct()
                .toList();
    }

    /**
     * 더 유연한 검색 - 부분 매칭, 유사어 검색 포함
     *
     * @param keyword 검색할 키워드
     * @return 키워드와 관련된 태그들의 리스트
     */
    public static List<String> findRelatedTags(String keyword) {
        List<String> result = new ArrayList<>();

        if (keyword == null || keyword.trim()
                .isEmpty()) {
            return result;
        }

        String lowerKeyword = keyword.toLowerCase()
                .trim();

        // 1. 직접 포함하는 태그들
        result.addAll(findTagsContainingKeyword(keyword));

        // 2. KEYWORD_MAPPINGS에서 관련 태그 찾기
        for (Map.Entry<String, Set<String>> entry : KEYWORD_MAPPINGS.entrySet()) {
            String tag = entry.getKey();
            Set<String> keywords = entry.getValue();

            // 키워드 매핑에서 해당 키워드를 포함하는 경우
            for (String mappedKeyword : keywords) {
                if (mappedKeyword.toLowerCase()
                        .contains(lowerKeyword) ||
                        lowerKeyword.contains(mappedKeyword.toLowerCase())) {
                    if (!result.contains(tag)) {
                        result.add(tag);
                    }
                    break;
                }
            }
        }

        return result;
    }

    /**
     * 카테고리별로 검색 결과 반환
     *
     * @param keyword 검색할 키워드
     * @return 카테고리별로 그룹화된 검색 결과
     */
    public static Map<String, List<String>> findTagsByCategoryContainingKeyword(String keyword) {
        Map<String, List<String>> result = new HashMap<>();

        if (keyword == null || keyword.trim()
                .isEmpty()) {
            return result;
        }

        String lowerKeyword = keyword.toLowerCase()
                .trim();

        for (Map.Entry<String, List<String>> entry : TAG_POOL.entrySet()) {
            String category = entry.getKey();
            List<String> tags = entry.getValue();
            List<String> matchedTags = new ArrayList<>();

            for (String tag : tags) {
                if (tag.toLowerCase()
                        .contains(lowerKeyword)) {
                    matchedTags.add(tag);
                }
            }

            if (!matchedTags.isEmpty()) {
                result.put(category, matchedTags);
            }
        }

        return result;
    }
}
