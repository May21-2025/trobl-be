package com.may21.trobl.admin.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl._global.enums.DateType;
import com.may21.trobl._global.security.JwtTokenUtil;
import com.may21.trobl.admin.AdminDto;
import com.may21.trobl.admin.DashboardDto;
import com.may21.trobl.admin.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final JwtTokenUtil jwtTokenUtil;
    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<Message> getDashboardStats(@RequestHeader("Authorization") String token) {
        jwtTokenUtil.getAdminUserByToken(token);
        DashboardDto.DashboardStats stats = dashboardService.getDashboardStats();
        return new ResponseEntity<>(Message.success(stats), HttpStatus.OK);
    }



    @GetMapping("/charts/user-growth")
    public ResponseEntity<Message> getUserGrowthData(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "dateType", defaultValue = "DAY") DateType dateType) {
        jwtTokenUtil.getAdminUserByToken(token);
        DashboardDto.UserGrowthDataDto response = dashboardService.getUserGrowthData(dateType);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/charts/community-posts")
    public ResponseEntity<Message> getCommunityPostsData(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "dateType", defaultValue = "DAY") DateType dateType) {
        jwtTokenUtil.getAdminUserByToken(token);
        DashboardDto.CommunityPostsDataDto response = dashboardService.getCommunityPostsData(dateType);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }

    @GetMapping("/charts/recording-ai-stats")
    public ResponseEntity<Message> getRecordingAIStatsData(
            @RequestHeader("Authorization") String token,
            @RequestParam(value = "dateType", defaultValue = "DAY") DateType dateType) {
        jwtTokenUtil.getAdminUserByToken(token);
        DashboardDto.RecordingAIStatsDataDto response = dashboardService.getRecordingAIStatsData(dateType);
        return new ResponseEntity<>(Message.success(response), HttpStatus.OK);
    }
}
