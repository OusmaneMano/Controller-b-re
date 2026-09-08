package com.controller.repository;

import com.controller.entity.Payment;
import com.controller.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByCompanyId(Long companyId);
    Page<Payment> findByCompanyId(Long companyId, Pageable pageable);
    List<Payment> findByStatus(PaymentStatus status);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}
