package com.may21.trobl.advertisement.repository;

import com.may21.trobl._global.enums.AdType;
import com.may21.trobl.advertisement.domain.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Advertisement, Long> {
    List<Advertisement> findActiveAdvertisements();
}
