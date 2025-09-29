package com.may21.trobl.admin.controller;

import com.may21.trobl._global.Message;
import com.may21.trobl.admin.service.TagMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tags/migration")
@RequiredArgsConstructor
public class TagMigrationController {

    private final TagMigrationService tagMigrationService;

    // 전체 데이터 마이그레이션
    @PostMapping("/all")
    public ResponseEntity<Message> migrateAllData() {
        tagMigrationService.migrateExistingData();
        return ResponseEntity.ok(Message.success("모든 태그 데이터가 성공적으로 마이그레이션되었습니다."));

    }

    // 특정 TagPool 데이터 마이그레이션
    @PostMapping("/pool/{poolName}")
    public ResponseEntity<Message> migrateTagPoolData(@PathVariable String poolName) {
        tagMigrationService.migrateTagPoolData(poolName);
        return ResponseEntity.ok(
                Message.success("TagPool '" + poolName + "' 데이터가 성공적으로 마이그레이션되었습니다."));

    }
}

