package com.may21.trobl.tag.repository;

import com.may21.trobl.post.domain.Posting;
import com.may21.trobl.tag.domain.Tag;
import com.may21.trobl.tag.domain.TagMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TagMappingRepository extends JpaRepository<TagMapping, Long> {

    @Query("SELECT tm.tag FROM TagMapping tm WHERE tm.posting = :post")
    List<Tag> getTagsByPost(Posting post);

    List<TagMapping> findByTag(Tag tagToDelete);

    List<TagMapping> findByPostingIn(List<Posting> postList);

    @Query("SELECT tm.tag FROM TagMapping tm WHERE tm.posting.id = :postId")
    List<Tag> getTagsByPostId(Long postId);

    @Query("SELECT tm FROM TagMapping tm WHERE tm.posting.id IN :postIds")
    List<TagMapping> findByPostIdIn(List<Long> postIds);
}
