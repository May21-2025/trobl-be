package com.may21.trobl.tag.repository;

import com.may21.trobl.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("SELECT t FROM Tag t WHERE t.id IN :existingTagIds OR t.name IN :tagNames")
    List<Tag> findAllByIdsAndNames(List<Long> existingTagIds, List<String> tagNames);

    @Query("SELECT t FROM Tag t WHERE t.id < 11")
    List<Tag> findStaticTags();

}
