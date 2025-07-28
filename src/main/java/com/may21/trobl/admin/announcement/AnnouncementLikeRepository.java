package com.may21.trobl.admin.announcement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementLikeRepository extends JpaRepository<AnnouncementLike, Long> {
    boolean existsByUserIdAndAnnouncementId(Long userId, Long announcementId);

    List<AnnouncementLike> findByAnnouncementId(Long announcementId);
}
