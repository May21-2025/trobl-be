package com.may21.trobl.admin.repository;

import com.may21.trobl.admin.domain.LayoutPostMapping;
import com.may21.trobl.admin.domain.MainLayoutGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LayoutPostMappingRepository  extends JpaRepository<LayoutPostMapping, Long> {

    @Modifying
    @Transactional
    void deleteAllByMainLayoutGroup(MainLayoutGroup mainLayoutGroup);

    @Query("SELECT lpm.postId FROM LayoutPostMapping lpm WHERE lpm.mainLayoutGroup = :mainLayoutGroup")
    List<Long> findPostIdsByMainLayoutGroup(MainLayoutGroup mainLayoutGroup);

    boolean existsByMainLayoutGroupAndPostId(MainLayoutGroup layout, Long postId);

    @Modifying
    @Transactional
    void deleteByMainLayoutGroupAndPostId(MainLayoutGroup layout, Long postId);
}
