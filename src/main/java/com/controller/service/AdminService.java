package com.controller.service;

import com.controller.dto.AuthDTO;
import com.controller.entity.Membership;
import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import com.controller.entity.enums.UserStatus;
import com.controller.exception.ResourceNotFoundException;
import com.controller.repository.MembershipRepository;
import com.controller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
        return userRepository.findAll().stream()
                .filter(u -> !"demo@controller.local".equalsIgnoreCase(u.getEmail()))
                .map(u -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", u.getId());
                    row.put("firstName", u.getFirstName());
                    row.put("lastName", u.getLastName());
                    row.put("email", u.getEmail());
                    row.put("role", u.getRole().toString());
                    row.put("status", u.getUserStatus() == null ? null : u.getUserStatus().toString());
                    row.put("paidUntil", u.getPaidUntil());
                    List<Membership> memberships = membershipRepository.findByUserId(u.getId());
                    row.put("companies", memberships.stream()
                            .map(m -> m.getCompany().getName() + " · " + m.getRole())
                            .distinct()
                            .toList());
                    long employees = memberships.stream()
                            .filter(m -> m.getRole() == UserRole.MANAGER && !Boolean.TRUE.equals(m.getCompany().getDemo()))
                            .mapToLong(m -> membershipRepository.findByCompanyIdAndRole(m.getCompany().getId(), UserRole.EMPLOYEE).size())
                            .sum();
                    row.put("employeeCount", employees);
                    row.put("monthlyFee", feeFor(employees));
                    return row;
                }).collect(Collectors.toList());
    }

    private int feeFor(long employees) {
        if (employees < 4) return 3000;
        if (employees <= 10) return 5000;
        if (employees <= 30) return 10000;
        return 20000;
    }

    /**
     * Payment any day in month M: month M+1 is free.
     * Next payment is due before the 10th of month M+2.
     * Account is blocked on the 11th of that month at 00:00.
     * Extra months push that date further out.
     * Example: paid in January for 1 month -> blocked 11 March 00:00.
     */
    public AuthDTO.UserInfo approveManager(Long userId, int months) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.MANAGER) throw new IllegalArgumentException("Only a manager can be approved");
        int period = Math.max(1, Math.min(months, 24));
        LocalDate today = LocalDate.now();
        LocalDate blockOn = today.withDayOfMonth(1).plusMonths(period + 1).withDayOfMonth(11);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setPaidUntil(LocalDateTime.of(blockOn, LocalTime.MIDNIGHT));
        return toInfo(userRepository.save(user));
    }

    public AuthDTO.UserInfo extend(Long userId, int months) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.MANAGER) throw new IllegalArgumentException("Only a manager can be extended");
        int period = Math.max(1, Math.min(months, 24));
        LocalDate base = user.getPaidUntil() != null && user.getPaidUntil().toLocalDate().isAfter(LocalDate.now())
                ? user.getPaidUntil().toLocalDate()
                : LocalDate.now();
        LocalDate blockOn = base.withDayOfMonth(1).plusMonths(period).withDayOfMonth(11);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setPaidUntil(LocalDateTime.of(blockOn, LocalTime.MIDNIGHT));
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