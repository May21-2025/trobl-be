package com.may21.trobl.admin.repository;

import com.may21.trobl._global.enums.PostingType;
import com.may21.trobl.admin.domain.PostDetailInfo;
import io.lettuce.core.dynamic.annotation.Param;
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

    @Query(value = """
        SELECT * FROM post_detail_info p 
        WHERE p.post_type IN :postingTypes 
        AND p.tags REGEXP :tagPattern
        """,
            countQuery = """
        SELECT COUNT(*) FROM post_detail_info p 
        WHERE p.post_type IN :postingTypes 
        AND p.tags REGEXP :tagPattern
        """,
            nativeQuery = true)
    Page<PostDetailInfo> findByPostTypeInAndTagsMatching(
            @Param("postingTypes") List<String> postingTypes,
            @Param("tagPattern") String tagPattern,
            Pageable pageable);
}
