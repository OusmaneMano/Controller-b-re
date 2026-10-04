package com.controller.service;

import com.controller.dto.AuthDTO;
import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import com.controller.entity.enums.UserStatus;
import com.controller.exception.ResourceNotFoundException;
import com.controller.repository.MembershipRepository;
import com.controller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public List<AuthDTO.UserInfo> listPendingManagers() {
        EnumSet<UserStatus> waiting = EnumSet.of(UserStatus.PAYMENT_SENT, UserStatus.PENDING_APPROVAL);
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.MANAGER && waiting.contains(u.getUserStatus()))
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> listAccounts() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId());
            row.put("firstName", u.getFirstName());
            row.put("lastName", u.getLastName());
            row.put("email", u.getEmail());
            row.put("role", u.getRole().toString());
            row.put("status", u.getUserStatus() == null ? null : u.getUserStatus().toString());
            row.put("paidUntil", u.getPaidUntil());
            row.put("companies", membershipRepository.findByUserId(u.getId()).stream()
                    .map(m -> m.getCompany().getName() + " · " + m.getRole())
                    .toList());
            return row;
        }).collect(Collectors.toList());
    }

    public AuthDTO.UserInfo approveManager(Long userId, int months) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.MANAGER) throw new IllegalArgumentException("Only a manager can be approved");
        int period = months < 1 ? 1 : Math.min(months, 12);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setPaidUntil(LocalDateTime.now().plusMonths(period));
        return toInfo(userRepository.save(user));
    }

    public AuthDTO.UserInfo block(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setUserStatus(UserStatus.SUSPENDED);
        user.setIsActive(false);
        return toInfo(userRepository.save(user));
    }

    public AuthDTO.UserInfo unblock(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setUserStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        return toInfo(userRepository.save(user));
    }

    private AuthDTO.UserInfo toInfo(User user) {
        return AuthDTO.UserInfo.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().toString())
                .status(user.getUserStatus() != null ? user.getUserStatus().toString() : null)
                .paymentNote(user.getPaymentNote())
                .build();
    }
}