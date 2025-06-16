package com.may21.trobl.post.domain;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<PollVote,Long> {

    Optional<PollVote> findByPollOptionIdAndUserId(Long pollOptionId, Long userId);

    @Modifying
    @Query("DELETE FROM PollVote p WHERE p.pollOption.id = :pollOptionId AND p.userId = :userId")
    void deleteByPollOptionIdAndUserId(@Param("pollOptionId") Long pollOptionId, @Param("userId") Long userId);

    @Query("SELECT DISTINCT p.pollOption.poll.posting FROM PollVote p WHERE p.userId = :userId")
    Page<Posting> findVotedPostsByUserId(Long userId, Pageable pageable);

    @Query("SELECT p.pollOption.id FROM PollVote p WHERE p.userId = :userId AND p.pollOption.poll.posting IN :posts")
    List<Long> findVotedOptionIdsByUserId(List<Posting> posts, Long userId);

    @Query("SELECT p.pollOption.id FROM PollVote p WHERE p.pollOption.poll.posting = :post AND p.userId = :userId")
    List<Long> findVotedPostByUserId(Posting post, Long userId);
}
