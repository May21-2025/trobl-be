package com.may21.trobl.admin;

import com.may21.trobl._global.enums.RoleType;
import com.may21.trobl._global.exception.BusinessException;
import com.may21.trobl._global.exception.ExceptionCode;
import com.may21.trobl.user.domain.User;
import com.may21.trobl.user.domain.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;

    @Transactional
    public boolean grantAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        List<RoleType> adminRoles = new ArrayList<>();
        adminRoles.add(RoleType.ADMIN);
        user.setRoles(adminRoles);

        userRepository.save(user);
        return true;
    }

    @Transactional
    public User revokeRole(Long userId, RoleType roleToRevoke) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        user.removeRole(roleToRevoke); // ⭐️ 특정 권한 제거

        return userRepository.save(user);
    }

    @Transactional
    public User updateAllRoles(Long userId, List<RoleType> newRoles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ExceptionCode.USER_NOT_FOUND));

        user.setRoles(newRoles); // ⭐️ 모든 권한을 새 리스트로 교체

        return userRepository.save(user);
    }
}
