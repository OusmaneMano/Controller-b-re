package com.controller.config;

import com.controller.entity.Company;
import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import com.controller.repository.CompanyRepository;
import com.controller.repository.UserRepository;
import com.controller.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class MembershipBackfill implements CommandLineRunner {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;

    @Override
    public void run(String... args) {
        int created = 0;
        for (Company company : companyRepository.findAll()) {
            if (Boolean.TRUE.equals(company.getDemo())) continue;
            User manager = company.getManager();
            if (manager != null) {
                membershipService.add(manager, company, UserRole.MANAGER);
                if (manager.getLastCompanyId() == null) {
                    manager.setLastCompanyId(company.getId());
                    userRepository.save(manager);
                }
                created++;
            }
        }
        for (User user : userRepository.findAll()) {
            if (user.getCompany() != null && user.getRole() == UserRole.EMPLOYEE) {
                membershipService.add(user, user.getCompany(), UserRole.EMPLOYEE);
                if (user.getLastCompanyId() == null) {
                    user.setLastCompanyId(user.getCompany().getId());
                    userRepository.save(user);
                }
            }
        }
        log.info("Membership backfill touched {} company-owner links", created);
    }
}
