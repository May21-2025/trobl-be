package com.may21.trobl.scheduler;

import com.may21.trobl.admin.service.AdminService;
import com.may21.trobl.admin.service.MainLayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Scheduler {

    @Autowired
    private AdminService adminService;

    @Autowired
    private MainLayoutService mainLayoutService;

    @Scheduled(cron = "0 0 0 * * *") // 매일 0시 0분 0초에 실행
    public void dailyJob() {
        adminService.makeTagsForPosts();
        adminService.updatePostDetailInfos();
        adminService.clearPostListCache();
        mainLayoutService.updateDailyLayoutPosts();
    }
    @Scheduled(cron = "0 0 0 * * MON")
    public void runEveryMonday() {
        mainLayoutService.updateWeeklyLayoutPosts();
    }
    @Scheduled(cron = "0 0 0 1 * ?")
    public void runEveryMonthFirstDay() {
        mainLayoutService.updateMonthlyLayoutPosts();
    }
    @Scheduled(cron = "0 0 0 1 1 ?")
    public void runEveryYearFirstDay() {
        mainLayoutService.updateYearlyLayoutPosts();
    }


}
