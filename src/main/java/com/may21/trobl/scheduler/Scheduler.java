package com.may21.trobl.scheduler;

import com.may21.trobl.admin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {

    @Autowired
    private AdminService adminService;

    @Scheduled(cron = "0 0 0 * * *") // 매일 0시 0분 0초에 실행
    public void dailyJob() {
        adminService.makeTagsForPosts();
        adminService.updatePostDetailInfos();
    }

}
