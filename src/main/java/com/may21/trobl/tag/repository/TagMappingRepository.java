package com.may21.trobl.tag.repository;

import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface TagMappingRepository extends JpaRepository<TagMapping, Long> {

    // N+1 문제 해결을 위한 페치 조인 쿼리들
    @Query("SELECT tm FROM TagMapping tm " +
           "JOIN FETCH tm.tag " +
           "JOIN FETCH tm.posting " +
           "WHERE tm.posting IN :postList AND (tm.admin = false OR tm.admin IS NULL)")
    List<TagMapping> findByPostingInWithTagAndPosting(@Param("postList") List<Posting> postList);

    @Query("SELECT tm FROM TagMapping tm " +
           "JOIN FETCH tm.tag " +
           "WHERE tm.posting.id IN :postIds AND (tm.admin = false OR tm.admin IS NULL)")
    List<TagMapping> findByPostIdInWithTag(@Param("postIds") List<Long> postIds);

    @Query("SELECT tm FROM TagMapping tm " +
           "JOIN FETCH tm.tag " +
           "WHERE tm.posting.id = :postId AND (tm.admin = false OR tm.admin IS NULL)")
    List<TagMapping> findByPostIdWithTag(@Param("postId") Long postId);

    // 기존 메서드들 (하위 호환성을 위해 유지)
    @Query("SELECT tm FROM TagMapping tm WHERE tm.tag = :tagToDelete AND (tm.admin = false OR tm.admin IS NULL)")
    List<TagMapping> findByTagAndAdminIsFalseOrAdminIsNull(@Param("tagToDelete") Tag tagToDelete);

    @Query("SELECT tm FROM TagMapping tm WHERE tm.posting IN :postList AND (tm.admin = false OR tm.admin IS NULL)")
    List<TagMapping> findByPostingInAndAdminIsFalseOrAdminIsNull(@Param("postList") List<Posting> postList);

    @Query("SELECT tm.tag FROM TagMapping tm WHERE tm.posting.id = :postId AND (tm.admin = false OR tm.admin IS NULL)")
    List<Tag> getTagsByPostIdAndNotAdmin(@Param("postId") Long postId);

    @Query("SELECT tm FROM TagMapping tm WHERE tm.posting.id IN :postIds AND (tm.admin = false OR tm.admin IS NULL)")
    List<TagMapping> findByPostIdIn(@Param("postIds") List<Long> postIds);

    @Query("SELECT tm FROM TagMapping tm WHERE tm.posting.id IN :postIds")
    List<TagMapping> findByPostIdInIncludingAdmin(@Param("postIds") List<Long> postIds);

    List<TagMapping> findAllByIdIn(List<Long> tagMappingIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM TagMapping tm WHERE tm.posting = :posting")
    int deleteByPosting(@Param("posting") Posting posting);

    @Modifying
    @Transactional
    void deleteAllByPostingInAndAdminIsTrue(Collection<Posting> posting);

    @Query("SELECT tm FROM TagMapping tm WHERE tm.posting IN :allPost AND (tm.admin = false OR tm.admin IS NULL)")
    List<TagMapping> findAllByPostingInAndAdminIsFalseOrAdminIsNull(@Param("allPost") List<Posting> allPost);

    @Query("SELECT DISTINCT tm.posting.id FROM TagMapping tm WHERE tm.tag.id IN :tagIds")
    List<Long> findPostIdsByTagIds(@Param("tagIds") List<Long> tagIds);

    // 페이징 지원 메서드들
    @Query("SELECT tm FROM TagMapping tm " +
           "JOIN FETCH tm.tag " +
           "WHERE tm.posting.id = :postId AND (tm.admin = false OR tm.admin IS NULL)")
    Page<TagMapping> findByPostIdWithTagPaged(@Param("postId") Long postId, Pageable pageable);
}
