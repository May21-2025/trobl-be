package com.may21.trobl.post.domain;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<PollVote,Long> {

    Optional<PollVote> findByPollOptionIdAndUserId(Long pollOptionId, Long userId);

    @Modifying
    @Query("DELETE FROM PollVote p WHERE p.pollOption.id = :pollOptionId AND p.userId = :userId")
    void deleteByPollOptionIdAndUserId(@Param("pollOptionId") Long pollOptionId, @Param("userId") Long userId);
}
