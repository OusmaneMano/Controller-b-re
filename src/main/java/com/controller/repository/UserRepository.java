package com.controller.repository;

import com.controller.entity.User;
import com.controller.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndRole(String email, UserRole role);
    List<User> findByCompanyId(Long companyId);
    List<User> findByCompanyIdAndRole(Long companyId, UserRole role);
    boolean existsByEmail(String email);
}
