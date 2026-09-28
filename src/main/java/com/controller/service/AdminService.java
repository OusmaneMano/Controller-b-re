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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal admin actions. Payment is still handled manually by you outside the
 * platform (WhatsApp/email, per your business model) - this is just the one
 * click that flips a manager's account to ACTIVE once you've received payment,
 * so they can log in and set up their company.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminService {

    private final UserRepository userRepository;

    public List<AuthDTO.UserInfo> listPendingManagers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.MANAGER && u.getUserStatus() == UserStatus.PENDING_APPROVAL)
                .map(u -> AuthDTO.UserInfo.builder()
                        .id(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .email(u.getEmail())
                        .role(u.getRole().toString())
                        .build())
                .collect(Collectors.toList());
    }

    public AuthDTO.UserInfo approveManager(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only manager accounts can be approved this way");
        }

        user.setUserStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        log.info("Manager {} approved by admin", saved.getEmail());

        return AuthDTO.UserInfo.builder()
                .id(saved.getId())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .email(saved.getEmail())
                .role(saved.getRole().toString())
                .build();
    }
}
