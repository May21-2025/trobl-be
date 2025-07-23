package com.may21.trobl.partner;

import com.may21.trobl._global.enums.RequestStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
public class PartnerRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private Long partnerId;
    private LocalDate marriageDate;
    private RequestStatus status;

    public PartnerRequest(Long userId, Long partnerId, LocalDate marriageDate) {
        this.userId = userId;
        this.partnerId = partnerId;
        this.status = RequestStatus.PENDING;
        this.marriageDate = marriageDate;
    }

    public void accept() {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }
        this.status = RequestStatus.ACCEPTED;
    }

    public void reject() {
        if (this.status != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }
        this.status = RequestStatus.REJECTED;
    }
}
