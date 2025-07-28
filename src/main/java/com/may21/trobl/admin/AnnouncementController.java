package com.may21.trobl.admin;

import com.may21.trobl._global.Message;
import com.may21.trobl.admin.service.AdminService;
import com.may21.trobl.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/announcement")
@RequiredArgsConstructor
@Slf4j
public class AnnouncementController {

    private final AdminService adminService;

    @GetMapping("/{announcementId}")
    public ResponseEntity<Message> getAnnouncementDetail(@PathVariable Long announcementId,
            @AuthenticationPrincipal User user) {
        Long userId = user==null? null: user.getId();
        AdminDto.AnnouncementDto response = adminService.getAnnouncement(announcementId, userId);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
