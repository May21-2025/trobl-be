package com.may21.trobl.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting,Long> {}
