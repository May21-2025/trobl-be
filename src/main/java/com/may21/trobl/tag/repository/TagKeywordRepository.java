package com.may21.trobl.tag.repository;

import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagKeywordRepository extends JpaRepository<TagKeyword, Long> {
    
    List<TagKeyword> findByTagId(Long tagId);
    
    void deleteByTagId(Long tagId);

    List<TagKeyword> findByTagIn(List<Tag> existingTagEntities);
    
    // 중복 키워드 확인을 위한 메서드
    boolean existsByTagIdAndKeyword(Long tagId, String keyword);
    
    // 키워드 ID로 삭제
    @Override
    void deleteById(Long keywordId);
    
    // 키워드가 있는 태그들의 ID 목록 조회
    @Query("SELECT DISTINCT tk.tag.id FROM TagKeyword tk")
    List<Long> findDistinctTagIds();
}
