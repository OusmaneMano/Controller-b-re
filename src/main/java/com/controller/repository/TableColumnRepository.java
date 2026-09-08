package com.controller.repository;

import com.controller.entity.TableColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TableColumnRepository extends JpaRepository<TableColumn, Long> {
    List<TableColumn> findByCompanyId(Long companyId);
    List<TableColumn> findByCompanyIdOrderByOrderIndexAsc(Long companyId);
}
