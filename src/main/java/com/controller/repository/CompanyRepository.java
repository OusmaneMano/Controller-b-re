package com.controller.repository;

import com.controller.entity.Company;
import com.controller.entity.User;
import com.controller.entity.enums.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String name);
    Optional<Company> findByManager(User manager);
    List<Company> findByStatus(CompanyStatus status);
    List<Company> findByManagerId(Long managerId);
    Optional<Company> findByEmployeeUsername(String employeeUsername);
}
