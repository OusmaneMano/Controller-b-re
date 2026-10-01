package com.controller.repository;

import com.controller.entity.Membership;
import com.controller.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByUserId(Long userId);
    Optional<Membership> findByUserIdAndCompanyId(Long userId, Long companyId);
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);
    List<Membership> findByCompanyIdAndRole(Long companyId, UserRole role);
    long countByUserIdAndRole(Long userId, UserRole role);
}
