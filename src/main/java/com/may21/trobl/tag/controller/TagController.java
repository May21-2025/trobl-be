package com.may21.trobl.tag.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("")
    public ResponseEntity<Message> getTags(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 모든 태그 조회
        Page<TagDto.Response> response = tagService.getAllTags(pageable);
        return ResponseEntity.ok(Message.success(response));
    }

    @GetMapping("/search")
    public ResponseEntity<Message> searchTags(
            @RequestParam @NotBlank @Size(min = 1, max = 50) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TagDto.Response> response = tagService.searchTags(keyword, pageable);
        return ResponseEntity.ok(Message.success(response));
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
    @GetMapping("/tag-pools/{tagPoolId}/tags")
    public ResponseEntity<Message> getTagsByTagPool(
            @PathVariable Long tagPoolId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<TagDto.TagInfo> response = tagService.getTagsInfoByTagPoolId(tagPoolId, pageable);
        return ResponseEntity.ok(Message.success(response));
    }

    @GetMapping("/tag-pools/tags")
    public ResponseEntity<Message> getTagsWithoutPool(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        Page<TagDto.TagInfo> response = tagService.getTagsInfoByTagPoolId(null, pageable);
        return ResponseEntity.ok(Message.success(response));
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
            @RequestParam Long tagPoolId) {
        TagDto.TagInfo response = tagService.updateTagPoolOfTag(tagId, tagPoolId);
        return ResponseEntity.ok(Message.success(response));
    }

    // 배치 처리 API
    @PutMapping("/batch/tag-pools")
    public ResponseEntity<Message> updateTagsBatch(
            @RequestParam List<Long> tagIds,
            @RequestParam(required = false) Long tagPoolId) {
        tagService.updateTagsBatch(tagIds, tagPoolId);
        return ResponseEntity.ok(Message.success("태그 풀 배치 업데이트가 완료되었습니다."));
    }


}
