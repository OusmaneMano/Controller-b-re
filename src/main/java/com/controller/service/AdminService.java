package com.controller.service;

import com.controller.dto.AuthDTO;
import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import com.controller.entity.enums.UserStatus;
import com.controller.exception.ResourceNotFoundException;
import com.controller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {

    private final UserRepository userRepository;

    public List<AuthDTO.UserInfo> listPendingManagers() {
        EnumSet<UserStatus> waiting = EnumSet.of(UserStatus.PAYMENT_SENT, UserStatus.PENDING_APPROVAL);
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.MANAGER && waiting.contains(u.getUserStatus()))
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    public AuthDTO.UserInfo approveManager(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only manager accounts can be approved this way");
        }
        user.setUserStatus(UserStatus.ACTIVE);
        return toInfo(userRepository.save(user));
    }

    private AuthDTO.UserInfo toInfo(User u) {
        return AuthDTO.UserInfo.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .role(u.getRole().toString())
                .status(u.getUserStatus() != null ? u.getUserStatus().toString() : null)
                .paymentNote(u.getPaymentNote())
                .build();
    }
}
