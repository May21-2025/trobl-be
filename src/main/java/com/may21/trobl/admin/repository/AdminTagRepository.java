package com.may21.trobl.admin.repository;

import com.may21.trobl.admin.domain.AdminTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminTagRepository extends JpaRepository<AdminTag, Long> {

    @Query("SELECT at FROM AdminTag at WHERE at.postId IN :postIds")
    List<AdminTag> findByPostIds(List<Long> postIds);
}
