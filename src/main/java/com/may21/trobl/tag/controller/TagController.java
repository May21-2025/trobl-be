package com.may21.trobl.tag.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.tag.dto.TagDto;
import com.may21.trobl.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
