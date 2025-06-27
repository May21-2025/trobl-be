package com.may21.trobl.advertisement.repository;

import com.may21.trobl.advertisement.domain.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Advertisement, Long> {

    @Query("SELECT a FROM Advertisement a WHERE a.active = true AND a.startDate <= CURRENT_DATE AND a.endDate >= CURRENT_DATE")
    List<Advertisement> findActiveAdvertisements();
}
