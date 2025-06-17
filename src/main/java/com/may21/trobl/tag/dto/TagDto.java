package com.may21.trobl.tag.dto;

import com.may21.trobl.tag.domain.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class TagDto {

    @Getter
    @AllArgsConstructor
    public static class Request{
        private Long tagId;
        private String name;
        private String color;
    }

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long tagId;
        private String name;
        private int count;

        public static List<Response> fromTagList(List<Tag> tags) {
            return tags.stream()
                    .map(tag -> new Response(tag.getId(), tag.getName(), tag.getCount()))
                    .toList();
        }
    }
}
