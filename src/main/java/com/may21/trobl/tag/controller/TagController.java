package com.may21.trobl.tag.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("")
    public ResponseEntity<Message> getTags() {
        List<TagDto.Response> response = tagService.getStaticTags();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<Message> searchTags(@RequestParam String keyword) {
        List<TagDto.Response> response = tagService.searchTags(keyword);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/organizations")
    public ResponseEntity<Message> getOrganizationTags() {
        boolean response = tagService.organize();
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @GetMapping("/tag-pools")
    public ResponseEntity<Message> getTagPools(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy) {
        Page<TagDto.TagPoolDto> response = tagService.getTagPools(page,size,sortBy);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @GetMapping("/tag-pools/tags")
    public ResponseEntity<Message> getTagPools(
            @RequestParam(required = false) Long tagPoolId) {
        List<TagDto.TagInfo> response = tagService.getTagsInfoByTagPoolId(tagPoolId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @DeleteMapping("/tag-pools/{tagPoolId}")
    public ResponseEntity<Message> deleteTagPool(
            @PathVariable Long tagPoolId) {
        boolean response = tagService.deleteTagPool(tagPoolId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<Message> createTag(
            @RequestParam String tagName,
            @RequestParam(required = false) Long tagPoolId) {
        TagDto.TagInfo response = tagService.createTag(tagName, tagPoolId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @PostMapping("/tag-pools")
    public ResponseEntity<Message> createTagPool(
            @RequestParam String tagPoolName) {
        TagDto.TagPoolDto response = tagService.createTagPool(tagPoolName);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/{tagId}/tag-pools")
    public ResponseEntity<Message> updateTagPool(@PathVariable Long tagId,
            @RequestParam(required = false) Long tagPoolId) {
        TagDto.TagInfo response = tagService.updateTagPoolOfTag(tagId,tagPoolId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

}
