package com.may21.trobl.advertisement.repository;

import com.may21.trobl.advertisement.domain.AdRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdRecordRepository extends JpaRepository<AdRecord, Long> {}
