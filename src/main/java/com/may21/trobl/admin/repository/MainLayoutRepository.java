package com.may21.trobl.admin.repository;

import com.may21.trobl.admin.domain.MainLayoutGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MainLayoutRepository extends JpaRepository<MainLayoutGroup, Long> {
    boolean existsByCode(String code);

    @Query("SELECT COUNT(m) FROM MainLayoutGroup m")
    int findMaxIndex();


    @Transactional
    @Modifying
    @Query("DELETE FROM MainLayoutGroup m WHERE m.code = :code")
    void deleteByCode(String code);
}
