package com.may21.trobl.admin;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.admin.service.AdminService;
import com.may21.trobl.post.dto.PostDto;
import com.may21.trobl.post.service.PostingService;
import com.may21.trobl.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
@Slf4j
public class AnnouncementController {

    private final PostingService postingService;
    private final JwtTokenUtil jwtTokenUtil;

    @GetMapping("")
    public ResponseEntity<Message> getAnnouncementDetail(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String token){
    Long userId = jwtTokenUtil.getUserIdFromToken(token);
        Page<AdminDto.AnnouncementList> response = postingService.getAnnouncements(userId, page,
                size);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
    @GetMapping("/{postId}")
    public ResponseEntity<Message> getAnnouncementDetail(@PathVariable Long postId,
            @AuthenticationPrincipal User user) {
        Long userId = user==null? null: user.getId();
        AdminDto.AnnouncementDto response = postingService.getAnnouncement(postId, userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
