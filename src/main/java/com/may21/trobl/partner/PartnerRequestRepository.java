package com.may21.trobl.partner;

import com.may21.trobl._global.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerRequestRepository extends JpaRepository<PartnerRequest, Long> {

    @Query("SELECT pr FROM PartnerRequest pr WHERE pr.userId = :userId OR pr.partnerId = :userId " +
            "ORDER BY pr.id DESC")
    List<PartnerRequest> findByUserId(Long userId);

    boolean existsByUserIdAndPartnerIdAndStatus(Long id, Long id1, RequestStatus requestStatus);

    Optional<PartnerRequest> findByIdAndStatus(Long partnerRequestId, RequestStatus requestStatus);
}
