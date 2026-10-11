package com.controller.repository;

import com.controller.entity.Membership;
import com.controller.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByUserId(Long userId);
    List<Membership> findByCompanyId(Long companyId);
    List<Membership> findByCompanyIdAndRole(Long companyId, UserRole role);
    long countByCompanyIdAndRole(Long companyId, UserRole role);
    Optional<Membership> findByUserIdAndCompanyId(Long userId, Long companyId);
}