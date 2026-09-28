package com.controller.repository;

import com.controller.entity.TableColumn;
import com.controller.entity.enums.FieldRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableColumnRepository extends JpaRepository<TableColumn, Long> {
    List<TableColumn> findByCompanyId(Long companyId);
    List<TableColumn> findByCompanyIdOrderByOrderIndexAsc(Long companyId);
    Optional<TableColumn> findByCompanyIdAndFieldRole(Long companyId, FieldRole fieldRole);
    void deleteByCompanyIdAndId(Long companyId, Long id);
}
