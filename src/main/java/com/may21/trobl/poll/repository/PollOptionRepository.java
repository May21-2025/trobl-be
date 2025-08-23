package com.may21.trobl.poll.repository;

import com.may21.trobl.poll.domain.Poll;
import com.may21.trobl.poll.domain.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findAllByPollIn(List<Poll> polls);

    List<PollOption> findByPollId(Long pollId);

    @Query("SELECT po FROM PollOption po " +
            "JOIN FETCH po.poll p " +
            "WHERE p.posting.id = :postId")
    List<PollOption> findByPostId(Long postId);

    @Query("SELECT po.poll.posting.id FROM PollOption po " +
            "WHERE po.id = :pollOptionId")
    Long findPostIdById(Long pollOptionId);
}
