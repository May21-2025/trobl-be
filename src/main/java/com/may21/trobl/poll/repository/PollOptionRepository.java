package com.may21.trobl.poll.repository;

import com.may21.trobl.poll.domain.Poll;
import com.may21.trobl.poll.domain.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findAllByPollIn(List<Poll> polls);
}
