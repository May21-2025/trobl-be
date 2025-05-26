package com.may21.trobl.post.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FairViewRepository extends JpaRepository<FairView,Long> {}
