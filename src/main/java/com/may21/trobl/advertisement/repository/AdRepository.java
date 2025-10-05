package com.may21.trobl.advertisement.repository;

import com.may21.trobl.advertisement.domain.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Advertisement, Long> {

    @Query("SELECT a FROM Advertisement a WHERE a.active = true AND a.startDate <= CURRENT_DATE AND a.endDate >= CURRENT_DATE")
    List<Advertisement> findActiveAdvertisements();


    Page<Advertisement> findAll(Pageable pageable);

    // 브랜드명으로 광고 조회
    List<Advertisement> findByBrandName(String brandName);

    // 활성 광고 수 조회
    long countByActiveTrue();
}
