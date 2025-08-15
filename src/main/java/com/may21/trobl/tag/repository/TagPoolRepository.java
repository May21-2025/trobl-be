package com.may21.trobl.tag.repository;

import com.may21.trobl.tag.domain.TagPool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagPoolRepository extends JpaRepository<TagPool, Long> {
    
    Optional<TagPool> findByName(String name);
    
    boolean existsByName(String name);
}
