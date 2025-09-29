package com.may21.trobl.admin.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/tags")
@RequiredArgsConstructor
public class TagKeywordController {

    private final TagService tagService;

    // 키워드 관련 API
    @PostMapping("/{tagId}/keywords")
    public ResponseEntity<Message> addKeywordToTag(@PathVariable Long tagId,
            @RequestBody Map<String, String> request) {
        String keyword = request.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }
        
        Long keywordId = tagService.addKeywordToTag(tagId, keyword);
        Map<String, Object> responseData = Map.of("keywordId", keywordId);
        return ResponseEntity.ok(Message.success(responseData));
    }

    @DeleteMapping("/keywords/{keywordId}")
    public ResponseEntity<Message> removeKeywordFromTag(@PathVariable Long keywordId) {
        tagService.removeKeywordFromTag(keywordId);
        return ResponseEntity.ok(Message.success("키워드가 삭제되었습니다."));
    }

    @GetMapping("/{tagId}/keywords")
    public ResponseEntity<Message> getKeywordsByTag(@PathVariable Long tagId) {
        TagDto.Keywords keywords = tagService.getKeywordsByTag(tagId);
        return ResponseEntity.ok(Message.success(keywords));
    }

    @PostMapping("/analyze")
    public ResponseEntity<Message> analyzePostContent(@RequestBody Map<String, String> request) {
        String content = request.get("content");
        if (content == null || content.trim()
                .isEmpty()) {
            throw new BusinessException(ExceptionCode.INVALID_INPUT_VALUE);
        }

        List<String> recommendedTags = tagService.analyzePostContent(content);
        return ResponseEntity.ok(Message.success(recommendedTags));
    }

    @GetMapping("/with-keywords")
    public ResponseEntity<Message> getTagsWithKeywords() {
        List<Long> tagIds = tagService.getTagsWithKeywords();
        return ResponseEntity.ok(Message.success(tagIds));
    }
}

