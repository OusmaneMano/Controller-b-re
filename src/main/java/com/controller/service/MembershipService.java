package com.controller.service;

import com.controller.dto.AuthDTO;
import com.controller.entity.Company;
import com.controller.entity.Membership;
import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import com.controller.exception.ResourceNotFoundException;
import com.controller.repository.CompanyRepository;
import com.controller.repository.MembershipRepository;
import com.controller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    public Membership add(User user, Company company, UserRole role) {
        return membershipRepository.findByUserIdAndCompanyId(user.getId(), company.getId())
                .map(existing -> {
                    // Keep manager role if they already own it; otherwise keep existing.
                    return existing;
                })
                .orElseGet(() -> membershipRepository.save(Membership.builder()
                        .user(user)
                        .company(company)
                        .role(role)
                        .build()));
    }

    public List<AuthDTO.MembershipInfo> listFor(User user) {
        return membershipRepository.findByUserId(user.getId()).stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    public Membership require(String email, Long companyId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (companyId == null) {
            if (user.getLastCompanyId() != null) {
                companyId = user.getLastCompanyId();
            } else {
                List<Membership> all = membershipRepository.findByUserId(user.getId());
                if (all.isEmpty()) {
                    throw new IllegalArgumentException("You are not linked to a company yet. Send X-Company-Id or join a company.");
                }
                companyId = all.get(0).getCompany().getId();
            }
        }
        Long cid = companyId;
        Membership m = membershipRepository.findByUserIdAndCompanyId(user.getId(), cid)
                .orElseThrow(() -> new IllegalArgumentException("You do not belong to this company"));
        user.setLastCompanyId(cid);
        user.setCompany(m.getCompany());
        userRepository.save(user);
        return m;
    }

    public Company companyOrThrow(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    public AuthDTO.MembershipInfo toInfo(Membership m) {
        Company c = m.getCompany();
        return AuthDTO.MembershipInfo.builder()
                .companyId(c.getId())
                .companyName(c.getName())
                .industry(c.getIndustry())
                .role(m.getRole().toString())
                .demo(Boolean.TRUE.equals(c.getDemo()))
                .status(c.getStatus() != null ? c.getStatus().toString() : null)
                .build();
    }
}
