package com.controller.repository;

import com.controller.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    List<Record> findByCompanyId(Long companyId);
    Page<Record> findByCompanyId(Long companyId, Pageable pageable);
    List<Record> findByCompanyIdAndCreatedById(Long companyId, Long userId);
    Page<Record> findByCompanyIdAndCreatedById(Long companyId, Long userId, Pageable pageable);
    List<Record> findByCompanyIdAndCreatedAtBetween(Long companyId, LocalDateTime start, LocalDateTime end);
    Long countByCompanyId(Long companyId);
}
