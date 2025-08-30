package com.may21.trobl.admin.repository;

import com.may21.trobl._global.enums.ScheduleType;
import com.may21.trobl.admin.domain.MainLayoutGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MainLayoutRepository extends JpaRepository<MainLayoutGroup, Long> {

    @Query("SELECT COUNT(m) FROM MainLayoutGroup m")
    int findMaxIndex();

    List<MainLayoutGroup> findByIndexBetween(int i, int index);

    @Modifying
    @Transactional
    void deleteById(Long id);

    List<MainLayoutGroup> findByScheduleType(ScheduleType scheduleType);
}
