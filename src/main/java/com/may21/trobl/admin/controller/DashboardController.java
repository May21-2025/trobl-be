package com.may21.trobl.admin.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final JwtTokenUtil jwtTokenUtil;
    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<Message> getDashboardStats(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        AdminDto.DashboardStats stats = adminService.getDashboardStats();
        return new ResponseEntity<>(Message.success(stats), HttpStatus.OK);
    }
}
