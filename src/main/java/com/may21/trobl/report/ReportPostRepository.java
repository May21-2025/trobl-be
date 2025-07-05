package com.may21.trobl.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportPostRepository extends JpaRepository<ReportPost, Long> {
    int countReportPostByPostId(Long postId);
}
