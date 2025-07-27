package com.may21.trobl._global.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.may21.trobl._global.component.GlobalValues;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static com.may21.trobl._global.component.GlobalValues.USER_PROFILE_IMAGE_PATH;

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
            return PageRequest.of(page, size, Sort.by(sort).ascending());
        } else if (sortDirection.equalsIgnoreCase("desc")) {
            return PageRequest.of(page, size, Sort.by(sort).descending());
        } else {
            return Pageable.ofSize(size).withPage(page);
        }
    }

    public static String getUserProfileUrl(String imageKey) {
        if (imageKey == null || imageKey.isEmpty()) {
            return "";
        }
        return GlobalValues.getCdnUrl() + USER_PROFILE_IMAGE_PATH + imageKey;

    }
}
