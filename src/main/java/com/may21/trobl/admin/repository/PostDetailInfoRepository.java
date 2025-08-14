package com.may21.trobl.admin.repository;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.admin.domain.PostDetailInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostDetailInfoRepository extends JpaRepository<PostDetailInfo, Long> {

    List<PostDetailInfo> findAllByPostIdIn(List<Long> oldPostIds);

    @Query("SELECT p FROM PostDetailInfo p WHERE p.postType IN :postingTypes")
    Page<PostDetailInfo> findAllContainsTagsFilteredByTypes(List<PostingType> postingTypes, Pageable pageable);

    @Query("SELECT p FROM PostDetailInfo p WHERE p.postType IN :postingTypes AND p.tags LIKE %:tags%")
    Page<PostDetailInfo> findAllContainsTagsFilteredByTypesAndTags(List<String> tags, List<PostingType> postingTypes, Pageable pageable);
}
