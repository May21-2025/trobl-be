package com.may21.trobl.post.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FairViewRepository extends JpaRepository<FairView, Long> {

    @Query("SELECT f.posting FROM FairView f JOIN f.posting p WHERE f.userId = :userId AND  p.userId != :userId")
    Page<Posting> findPostsByUserId(Long userId, Pageable pageable);

   @Query("SELECT f FROM FairView f WHERE f.userId = :userId AND f.posting.id IN :postIds")
    List<FairView> findAllByUserIdAndPostIdIn(Long userId, List<Long> postIds);

    boolean existsByPostingAndConfirmedIsFalse(Posting post);

    List<FairView> findAllByPostingIdIn(List<Long> list);
}
