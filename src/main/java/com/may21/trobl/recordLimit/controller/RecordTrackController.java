package com.may21.trobl.recordLimit.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.recordLimit.dto.RecordDto;
import com.may21.trobl.recordLimit.service.RecordTrackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/record-track")
public class RecordTrackController {
    private final JwtTokenUtil jwtTokenUtil;
    private final RecordTrackService recordTrackService;


    @GetMapping("")
    public ResponseEntity<Message> getUserUsage(@RequestHeader("Authorization") String token) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token)
                .getId();
        RecordDto.Usage response = recordTrackService.getUserUsage(userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @PostMapping("")
    public ResponseEntity<Message> trackUserRecord(@RequestParam String recordId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token)
                .getId();
        boolean response = recordTrackService.trackUserRecord(userId,recordId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PutMapping("/ai-report")
    public ResponseEntity<Message> trackAiGeneration(@RequestParam String recordId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token)
                .getId();
        RecordDto.Usage response = recordTrackService.trackAiGeneration(userId,recordId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @PostMapping("/limit")
    public ResponseEntity<Message> increaseAiLimit(@RequestHeader("Authorization") String token,
            @RequestParam("type") String type) {
        Long userId = jwtTokenUtil.getUserFromValidateAccessToken(token)
                .getId();
        RecordDto.Usage response = recordTrackService.increaseAiLimit(userId,type);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
