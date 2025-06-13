package com.may21.trobl.post.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PollRepository extends JpaRepository<Poll, Long> {

    @Query("SELECT p FROM Poll p LEFT JOIN FETCH p.pollOptions WHERE p.posting IN :postList")
    List<Poll> findAllByPostIn(List<Posting> postList);

    List<Poll> findAllByPostingIn(List<Posting> posts);
}
