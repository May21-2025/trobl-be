package com.may21.trobl.poll.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/poll-options")
public class PollController {
    private final PostingService postingService;

    @PutMapping("/{pollOptionId}/vote")
    public ResponseEntity<Message> votePoll(
            @PathVariable Long pollOptionId, @AuthenticationPrincipal User user) {
        boolean response = postingService.votePoll(pollOptionId, user.getId());
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
