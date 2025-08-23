package com.may21.trobl.tag.repository;

import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface TagMappingRepository extends JpaRepository<TagMapping, Long> {

    @Query("SELECT tm.tag FROM TagMapping tm WHERE tm.tag = :tagToDelete")
    List<TagMapping> findByTagAndAdminIsFalseOrAdminIsNull(Tag tagToDelete);

    List<TagMapping> findByPostingInAndAdminIsFalseOrAdminIsNull(List<Posting> postList);

    @Query("SELECT tm.tag FROM TagMapping tm WHERE tm.posting.id = :postId AND (tm.admin = false " +
            "OR tm.admin IS NULL)")
    List<Tag> getTagsByPostIdAndNotAdmin(Long postId);

    @Query("SELECT tm FROM TagMapping tm WHERE tm.posting.id IN :postIds AND (tm.admin = false " +
            "           OR tm.admin IS NULL)")
    List<TagMapping> findByPostIdIn(List<Long> postIds);
    @Query("SELECT tm FROM TagMapping tm WHERE tm.posting.id IN :postIds")
    List<TagMapping> findByPostIdInIncludingAdmin(List<Long> postIds);

    List<TagMapping> findAllByIdIn(List<Long> tagMappingIds);

    @Modifying
    @Transactional
    void deleteAllByPostingInAndAdminIsTrue(Collection<Posting> posting);

    List<TagMapping> findAllByPostingInAndAdminIsFalseOrAdminIsNull(List<Posting> allPost);
}
