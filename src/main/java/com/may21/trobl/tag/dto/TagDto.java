package com.may21.trobl.tag.dto;

import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import com.may21.trobl.tag.domain.TagPool;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class TagDto {

    @Getter
    @AllArgsConstructor
    public static class Request {
        private Long tagId;
        @Setter
        private String name;
        private String color;
    }

    @Getter
    @AllArgsConstructor
    public static class Response {
        private Long tagId;
        private String name;

        public static List<Response> fromTagList(List<Tag> tags) {
            return tags.stream()
                    .map(tag -> new Response(tag.getId(), tag.getName()))
                    .toList();
        }
    }

    @Getter
    public static class TagMappingInfo extends TagInfo{
        private final boolean adminAdded;

        public TagMappingInfo(TagMapping tagMapping) {
            super(tagMapping);
            this.adminAdded = tagMapping.getAdmin();
        }

        public static List<TagMappingInfo> fromTagMappings(List<TagMapping> tags) {
            return tags.stream()
                    .map(TagMappingInfo::new)
                    .toList();
        }
    }

    @Getter
    public static class TagInfo {
        private final Long tagId;
        private final Long tagPoolId;
        private final String tagPoolName;
        private final String title;

        public TagInfo(TagMapping tagMapping) {
            Tag tag = tagMapping.getTag();
            TagPool tagPool = tag.getTagPool();
            this.tagId = tag.getId();
            this.title = tag.getName();
            this.tagPoolId = tagPool != null ? tagPool.getId() : null;
            this.tagPoolName = tagPool != null ? tagPool.getName() : null;
        }
        public TagInfo(Tag tag) {
            TagPool tagPool = tag.getTagPool();
            this.tagId = tag.getId();
            this.title = tag.getName();
            this.tagPoolId = tagPool != null ? tagPool.getId() : null;
            this.tagPoolName = tagPool != null ? tagPool.getName() : null;
        }
        public TagInfo(Tag tag, TagPool tagPool) {
            this.tagId = tag.getId();
            this.title = tag.getName();
            this.tagPoolId = tagPool != null ? tagPool.getId() : null;
            this.tagPoolName = tagPool != null ? tagPool.getName() : null;
        }
        public static List<TagInfo> fromTags(List<Tag> tags, TagPool tagPool) {
            return tags.stream()
                    .map(tag -> new TagInfo(tag, tagPool))
                    .toList();
        }

        public static List<TagInfo> fromTags(List<Tag> tags) {
            return tags.stream()
                    .map(TagInfo::new)
                    .toList();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class TagPoolDto {
        private final Long tagPoolId;
        private final String name;

        public TagPoolDto(TagPool tagPool) {
            this.tagPoolId = tagPool.getId();
            this.name = tagPool.getName();
        }
    }
}
