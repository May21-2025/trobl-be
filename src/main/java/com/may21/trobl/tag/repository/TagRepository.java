package com.may21.trobl.tag.repository;

import com.may21.trobl.tag.domain.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("SELECT t FROM Tag t WHERE t.id IN :existingTagIds OR t.name IN :tagNames")
    List<Tag> findAllByIdsAndNames(@Param("existingTagIds") List<Long> existingTagIds, 
                                   @Param("tagNames") List<String> tagNames);

    // 기존 메서드 (하위 호환성)
    @Query("SELECT t FROM Tag t WHERE t.id IN (" +
            "SELECT MIN(t2.id) FROM Tag t2 WHERE LOWER(t2.name) LIKE LOWER(CONCAT('%', :keyword, '%')) GROUP BY t2.name" +
            ")")
    List<Tag> findDistinctTagsByNameContaining(@Param("keyword") String keyword);

    // 페이징 지원 검색 메서드들
    @Query("SELECT t FROM Tag t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Tag> findTagsByNameContaining(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = """
        SELECT t.* FROM tag t 
        WHERE t.id IN (
            SELECT MIN(t2.id) 
            FROM tag t2 
            WHERE LOWER(t2.name) LIKE LOWER(CONCAT('%', :keyword, '%')) 
            GROUP BY t2.name
        )
        ORDER BY t.name
        """, nativeQuery = true)
    Page<Tag> findDistinctTagsByNameContainingPaged(@Param("keyword") String keyword, Pageable pageable);


    // 태그 풀별 조회 (페이징 지원)
    @Query("SELECT t FROM Tag t WHERE t.tagPool.id = :tagPoolId ORDER BY t.name")
    Page<Tag> findByTagPoolId(@Param("tagPoolId") Long tagPoolId, Pageable pageable);

    @Query("SELECT t FROM Tag t WHERE t.tagPool IS NULL ORDER BY t.name")
    Page<Tag> findByTagPoolIsNull(Pageable pageable);

    // 모든 태그 조회 (페이징 지원)
    @Query("SELECT t FROM Tag t ORDER BY t.name")
    Page<Tag> findAllTags(Pageable pageable);

    // 기존 메서드들 (하위 호환성)
    List<Tag> findAllByTagPoolIsNotNull();

    List<Tag> findByTagPoolId(Long tagPoolId);

    List<Tag> findByTagPoolIsNull();

    boolean existsByName(String tagName);

    @Modifying
    @Query("UPDATE Tag t SET t.tagPool = null WHERE t.tagPool.id = :tagPoolId")
    int clearTagPoolFromTags(@Param("tagPoolId") Long tagPoolId);

    // 배치 처리를 위한 페이징 쿼리
    @Query(value = "SELECT * FROM tag ORDER BY id LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Tag> findTagsWithPagination(@Param("offset") int offset, @Param("limit") int limit);

    // 배치 업데이트
    @Modifying
    @Query("UPDATE Tag t SET t.tagPool = :tagPool WHERE t.id IN :tagIds")
    int updateTagPoolBatch(@Param("tagIds") List<Long> tagIds, @Param("tagPool") com.may21.trobl.tag.domain.TagPool tagPool);
    
    // TagPool 이름으로 태그 조회
    List<Tag> findByTagPoolName(String tagPoolName);
    
    // 태그 이름으로 조회
    Optional<Tag> findByName(String name);

    @Query("select t.name from Tag t where t.name in :names")
    List<String> findNamesByNameIn(@Param("names") Set<String> names);

    List<Tag> findByNameIn(Set<String> allTagNames);
}
