package com.may21.trobl._global.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class Utility {


    public static String toJson(Map<String, String> data) {
        try {
            return new ObjectMapper().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public static String getRandomNickname() {
        //make 5개 소문자 영어와 숫자 3개의 조합으로 랜덤 닉네임 생성
        StringBuilder nickname = new StringBuilder();
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";
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
}
