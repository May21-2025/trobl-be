package com.may21.trobl.tag.repository;

import com.may21.trobl.tag.domain.Tag;
import org.checkerframework.common.aliasing.qual.Unique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("SELECT t FROM Tag t WHERE t.id IN :existingTagIds OR t.name IN :tagNames")
    List<Tag> findAllByIdsAndNames(List<Long> existingTagIds, List<String> tagNames);

    @Query("SELECT t FROM Tag t WHERE t.id < 11")
    List<Tag> findStaticTags();

    @Query("SELECT t FROM Tag t WHERE t.id IN (" +
            "SELECT MIN(t2.id) FROM Tag t2 WHERE LOWER(t2.name) LIKE LOWER(CONCAT('%', :keyword, '%')) GROUP BY t2.name" +
            ")")
    List<Tag> findDistinctTagsByNameContaining(String keyword);

    List<Tag> findAllByTagPoolIsNotNull();
}
