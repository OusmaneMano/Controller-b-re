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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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

    public List<AuthDTO.UserInfo> pendingManagers() {
        EnumSet<UserStatus> waiting = EnumSet.of(UserStatus.PAYMENT_SENT, UserStatus.PENDING_APPROVAL);
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.MANAGER && waiting.contains(u.getUserStatus()))
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> listAccounts() {
        return userRepository.findAll().stream()
                .filter(u -> !"demo@controller.local".equalsIgnoreCase(u.getEmail()))
                .sorted(Comparator.comparing(User::getId))
                .map(u -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", u.getId());
                    row.put("firstName", u.getFirstName());
                    row.put("lastName", u.getLastName());
                    row.put("email", u.getEmail());
                    row.put("role", u.getRole().toString());
                    row.put("status", u.getUserStatus() == null ? null : u.getUserStatus().toString());
                    row.put("paidUntil", u.getPaidUntil());
                    row.put("monthsLeft", monthsLeft(u.getPaidUntil()));
                    boolean expired = u.getRole() == UserRole.MANAGER
                            && (u.getPaidUntil() == null || !u.getPaidUntil().isAfter(LocalDateTime.now())
                            || u.getUserStatus() == UserStatus.SUSPENDED);
                    row.put("expired", expired);
                    List<Membership> memberships = membershipRepository.findByUserId(u.getId());
                    row.put("companies", memberships.stream()
                            .map(m -> m.getCompany().getName() + " · " + m.getRole())
                            .distinct()
                            .toList());
                    int limit = memberships.stream()
                            .filter(m -> m.getRole() == UserRole.MANAGER && !Boolean.TRUE.equals(m.getCompany().getDemo()))
                            .map(m -> m.getCompany().getEmployeeLimit() == null ? 3 : m.getCompany().getEmployeeLimit())
                            .findFirst().orElse(0);
                    row.put("employeeLimit", limit);
                    row.put("monthlyFee", feeFor(limit));
                    return row;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> paymentFor(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("status", user.getUserStatus() == null ? null : user.getUserStatus().toString());
        row.put("paidUntil", user.getPaidUntil());
        row.put("monthsLeft", monthsLeft(user.getPaidUntil()));
        row.put("daysLeft", user.getPaidUntil() == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), user.getPaidUntil().toLocalDate())));
        return row;
    }

    private int monthsLeft(LocalDateTime until) {
        if (until == null || !until.isAfter(LocalDateTime.now())) return 0;
        LocalDate end = until.toLocalDate();
        LocalDate now = LocalDate.now();
        int months = (end.getYear() - now.getYear()) * 12 + (end.getMonthValue() - now.getMonthValue());
        if (end.getDayOfMonth() < now.getDayOfMonth()) months--;
        return Math.max(1, months);
    }

    private int feeFor(long employees) {
        if (employees < 4) return 3000;
        if (employees <= 10) return 5000;
        if (employees <= 30) return 10000;
        return 20000;
    }

    public AuthDTO.UserInfo approveManager(Long userId, int months, int employeeLimit) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.MANAGER) throw new IllegalArgumentException("Only a manager can be approved");
        int period = Math.max(1, Math.min(months, 12));
        int limit = Math.max(3, Math.min(employeeLimit, 100));
        LocalDate blockOn = LocalDate.now().withDayOfMonth(1).plusMonths(period).withDayOfMonth(11);
        user.setUserStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setPaidUntil(LocalDateTime.of(blockOn, LocalTime.MIDNIGHT));
        userRepository.save(user);
        applyLimit(user, limit);
        setStaffActive(user, true);
        return toInfo(user);
    }

    public AuthDTO.UserInfo increment(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        LocalDate base = user.getPaidUntil() != null && user.getPaidUntil().toLocalDate().isAfter(LocalDate.now())
                ? user.getPaidUntil().toLocalDate()
                : LocalDate.now().withDayOfMonth(1).plusMonths(1).withDayOfMonth(11);
        user.setPaidUntil(LocalDateTime.of(base.plusMonths(1), LocalTime.MIDNIGHT));
        user.setUserStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        userRepository.save(user);
        setStaffActive(user, true);
        return toInfo(user);
    }

    public AuthDTO.UserInfo decrement(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getPaidUntil() == null) return toInfo(user);
        LocalDate next = user.getPaidUntil().toLocalDate().minusMonths(1);
        LocalDate minOne = LocalDate.now().withDayOfMonth(1).plusMonths(1).withDayOfMonth(11);
        if (next.isBefore(minOne)) next = minOne;
        user.setPaidUntil(LocalDateTime.of(next, LocalTime.MIDNIGHT));
        userRepository.save(user);
        return toInfo(user);
    }

    public AuthDTO.UserInfo setStaffLimit(Long userId, int employeeLimit) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != UserRole.MANAGER) throw new IllegalArgumentException("Only a manager has a staff limit");
        applyLimit(user, Math.max(3, Math.min(employeeLimit, 100)));
        return toInfo(user);
    }

    private void applyLimit(User manager, int limit) {
        for (Membership m : membershipRepository.findByUserId(manager.getId())) {
            if (m.getRole() == UserRole.MANAGER && !Boolean.TRUE.equals(m.getCompany().getDemo())) {
                m.getCompany().setEmployeeLimit(limit);
            }
        }
    }

    public AuthDTO.UserInfo block(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setUserStatus(UserStatus.SUSPENDED);
        user.setIsActive(false);
        userRepository.save(user);
        setStaffActive(user, false);
        return toInfo(user);
    }

    public AuthDTO.UserInfo unblock(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setUserStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        userRepository.save(user);
        setStaffActive(user, true);
        return toInfo(user);
    }

    private void setStaffActive(User manager, boolean active) {
        if (manager.getRole() != UserRole.MANAGER) return;
        for (Membership m : membershipRepository.findByUserId(manager.getId())) {
            if (m.getRole() != UserRole.MANAGER || Boolean.TRUE.equals(m.getCompany().getDemo())) continue;
            for (Membership staff : membershipRepository.findByCompanyIdAndRole(m.getCompany().getId(), UserRole.EMPLOYEE)) {
                User employee = staff.getUser();
                if (employee == null || employee.getRole() == UserRole.ADMIN) continue;
                employee.setUserStatus(active ? UserStatus.ACTIVE : UserStatus.SUSPENDED);
                employee.setIsActive(active);
                userRepository.save(employee);
            }
        }
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

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() == UserRole.ADMIN) throw new IllegalArgumentException("Cannot delete the admin");
        try {
            membershipRepository.deleteAll(membershipRepository.findByUserId(user.getId()));
            userRepository.delete(user);
        } catch (Exception e) {
            // Soft delete if hard delete fails (foreign keys)
            user.setUserStatus(UserStatus.DELETED);
            user.setIsActive(false);
            userRepository.save(user);
        }
    }
}