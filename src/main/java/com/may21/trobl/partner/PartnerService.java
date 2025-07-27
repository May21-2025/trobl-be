package com.may21.trobl.partner;

import com.may21.trobl._global.enums.RequestStatus;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.notification.service.NotificationService;
import com.may21.trobl.user.UserDto;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerService {
    private final PartnerRequestRepository partnerRequestRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public UserDto.PartnerInfo getRequestStatus(Long userId) {
        List<PartnerRequest> request = partnerRequestRepository.findByUserId(userId);
        if (request.isEmpty()) {
            return null;
        }
        User me = userRepository.findById(userId)
                .orElse(null);
        PartnerRequest partnerRequest = request.getFirst();
        User partner = null;
        Long requesterId = partnerRequest.getUserId();
        Long receiverId = partnerRequest.getPartnerId();
        if (requesterId.equals(userId)) {
            partner = userRepository.findById(receiverId)
                    .orElse(null);
        }
        else if (receiverId.equals(userId)) {
            partner = userRepository.findById(requesterId)
                    .orElse(null);
        }
        if (partner == null || me == null) {
            partnerRequestRepository.delete(partnerRequest);
            return null;
        }
        return new UserDto.PartnerInfo(me, partner, partnerRequest);
    }

    public boolean requestPartner(Long userId, UserDto.RequestPartner request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        if (user.getPartnerId() != null)
            throw new BusinessException(ExceptionCode.USER_ALREADY_HAS_PARTNER);
        String username = request.username();
        User partner = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
        if (partner.getPartnerId() != null)
            throw new BusinessException(ExceptionCode.REQUESTED_PARTNER_ALREADY_HAS_PARTNER);
        if (user.getId()
                .equals(partner.getId())) {
            throw new BusinessException(ExceptionCode.CANNOT_REQUEST_SELF);
        }
        if (partnerRequestRepository.existsByUserIdAndPartnerIdAndStatus(user.getId(),
                partner.getId(), RequestStatus.PENDING)) {
            throw new BusinessException(ExceptionCode.PARTNER_REQUEST_ALREADY_EXISTS);
        }
        PartnerRequest partnerRequest =
                new PartnerRequest(userId, partner.getId(), request.marriageDate());
        partnerRequestRepository.save(partnerRequest);
        notificationService.sendPartnerRequest(partner, user);
        return true;
    }

    @Transactional
    public boolean matchPartner(Long userid, Long partnerRequestId,
            UserDto.AcceptPartnerRequest request) {
        PartnerRequest partnerRequest =
                partnerRequestRepository.findByIdAndStatus(partnerRequestId, RequestStatus.PENDING)
                        .orElse(null);
        if (partnerRequest == null || partnerRequest.getStatus() != RequestStatus.PENDING ||
                !Objects.equals(partnerRequest.getPartnerId(), userid)) {
            throw new BusinessException(ExceptionCode.INVALID_REQUEST);
        }
        //check if the request's partnerId matches the given userId
        if (request.accepted()) {
            if (partnerRequest.getMarriageDate() == null || !partnerRequest.getMarriageDate()
                    .isEqual(request.marriageDate())) {
                //marriageDate example: 2023-10-21
                throw new BusinessException(ExceptionCode.MARRIAGE_DATE_IS_NOT_SAME);
            }
            User user = userRepository.findById(partnerRequest.getUserId())
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
            User partner = userRepository.findById(partnerRequest.getPartnerId())
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
            if (user.getPartnerId() != null || partner.getPartnerId() != null) {
                throw new BusinessException(ExceptionCode.RESTRICTED, "이미 배우자가 존재합니다.");
            }
            user.setPartner(partner);
            partner.setPartner(user);
            partnerRequest.accept();
            notificationService.sendPartnerAccepted(user, partner);
        }
        else {
            partnerRequest.reject();
            User user = userRepository.findById(partnerRequest.getUserId())
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
            User partner = userRepository.findById(partnerRequest.getPartnerId())
                    .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));
            notificationService.sendPartnerDeclined(user, partner);
        }
        return true;
    }

    public boolean checkMarriageDate(Long partnerRequestId, LocalDate marriageDate) {
        PartnerRequest partnerRequest = partnerRequestRepository.findById(partnerRequestId)
                .orElse(null);
        if (partnerRequest == null) {
            return false;
        }
        return partnerRequest.getMarriageDate() != null && partnerRequest.getMarriageDate()
                .isEqual(marriageDate);
    }

    public boolean deleteMarriageInfo(Long partnerRequestId) {
        PartnerRequest partnerRequest = partnerRequestRepository.findById(partnerRequestId)
                .orElse(null);
        if (partnerRequest == null) {
            return false;
        }
        partnerRequestRepository.delete(partnerRequest);
        return true;
    }
}
