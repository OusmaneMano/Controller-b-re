package com.controller.service;

import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import com.controller.entity.enums.UserStatus;
import com.controller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final UserRepository userRepository;
    private final AdminService adminService;

    @Scheduled(cron = "0 5 0 * * ?")
    @Transactional
    public void blockOverdueManagers() {
        LocalDateTime now = LocalDateTime.now();
        for (User user : userRepository.findAll()) {
            if (user.getRole() != UserRole.MANAGER) continue;
            if (user.getUserStatus() == UserStatus.SUSPENDED || user.getUserStatus() == UserStatus.DELETED) continue;
            LocalDateTime until = user.getPaidUntil();
            if (until == null || !until.isAfter(now)) {
                adminService.block(user.getId());
                log.info("Auto-blocked overdue manager {}", user.getEmail());
            }
        }
    }
}