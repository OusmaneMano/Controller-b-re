package com.controller.service;

import com.controller.dto.CompanyDTO;
import com.controller.entity.Company;
import com.controller.entity.TableColumn;
import com.controller.entity.User;
import com.controller.entity.enums.FieldRole;
import com.controller.entity.enums.FieldType;
import com.controller.entity.enums.UserRole;
import com.controller.entity.enums.UserStatus;
import com.controller.exception.ResourceNotFoundException;
import com.controller.repository.CompanyRepository;
import com.controller.repository.TableColumnRepository;
import com.controller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final TableColumnRepository tableColumnRepository;
    private final UserRepository userRepository;

    /**
     * Full company setup: company info + the Quantity/Unit Price/Amount choice
     * + custom columns + table design + employee credentials, in one call.
     */
    public CompanyDTO.CompanyResponse setupCompany(String managerEmail, CompanyDTO.SetupRequest request) {
        User manager = getManagerOrThrow(managerEmail);

        if (companyRepository.findByManager(manager).isPresent()) {
            throw new IllegalArgumentException("This manager already has a company set up");
        }

        if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }
        if (request.getEmployeeUsername() == null || request.getEmployeeUsername().isBlank()) {
            throw new IllegalArgumentException("Employee username is required");
        }
        if (request.getEmployeePassword() == null || request.getEmployeePassword().length() < 4
                || !request.getEmployeePassword().matches("\\d+")) {
            throw new IllegalArgumentException("Employee password must be 4 or more digits");
        }

        Company company = Company.builder()
                .name(request.getCompanyName())
                .industry(request.getIndustry())
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .manager(manager)
                .employeeUsername(request.getEmployeeUsername())
                .employeePassword(request.getEmployeePassword())
                .tableDesign(request.getTableDesign() != null ? request.getTableDesign() : "PROFESSIONAL")
                .build();

        Company savedCompany = companyRepository.save(company);

        int orderIndex = 0;
        List<TableColumn> columns = new ArrayList<>();

        // Auto-added Quantity/Unit Price/Amount columns, based on the manager's choice
        CompanyDTO.AmountColumnsOption option = request.getAmountColumnsOption();
        if (option == CompanyDTO.AmountColumnsOption.QUANTITY_UNIT_PRICE_AMOUNT) {
            columns.add(buildSystemColumn(savedCompany, "Quantity", FieldType.NUMBER, FieldRole.QUANTITY, orderIndex++));
            columns.add(buildSystemColumn(savedCompany, "Unit Price", FieldType.NUMBER, FieldRole.UNIT_PRICE, orderIndex++));
            columns.add(buildSystemColumn(savedCompany, "Amount", FieldType.NUMBER, FieldRole.AMOUNT, orderIndex++));
        } else if (option == CompanyDTO.AmountColumnsOption.AMOUNT_ONLY) {
            columns.add(buildSystemColumn(savedCompany, "Amount", FieldType.NUMBER, FieldRole.AMOUNT, orderIndex++));
        }
        // NONE (or null) -> no auto columns added

        // Manager's own custom columns
        if (request.getCustomColumns() != null) {
            for (CompanyDTO.ColumnInput input : request.getCustomColumns()) {
                if (input.getFieldName() == null || input.getFieldName().isBlank()) {
                    continue;
                }
                columns.add(TableColumn.builder()
                        .company(savedCompany)
                        .fieldName(input.getFieldName())
                        .fieldType(input.getFieldType() != null ? input.getFieldType() : FieldType.TEXT)
                        .fieldRole(FieldRole.NONE)
                        .isRequired(input.getIsRequired() != null && input.getIsRequired())
                        .orderIndex(orderIndex++)
                        .description(input.getDescription())
                        .defaultValue(input.getDefaultValue())
                        .validationRules(input.getValidationRules())
                        .options(input.getOptions())
                        .build());
            }
        }

        List<TableColumn> savedColumns = tableColumnRepository.saveAll(columns);
        log.info("Company '{}' set up by manager {} with {} columns", savedCompany.getName(), managerEmail, savedColumns.size());

        return toCompanyResponse(savedCompany, savedColumns, "Company set up successfully");
    }

    public CompanyDTO.CompanyResponse getMyCompany(String managerEmail) {
        User manager = getManagerOrThrow(managerEmail);
        Company company = companyRepository.findByManager(manager)
                .orElseThrow(() -> new ResourceNotFoundException("No company set up yet for this manager"));
        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(company.getId());
        return toCompanyResponse(company, columns, null);
    }

    public List<CompanyDTO.ColumnResponse> getColumnsForEmployee(String employeeEmail) {
        User employee = userRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (employee.getCompany() == null) {
            throw new IllegalArgumentException("You are not linked to a company yet");
        }
        return tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(employee.getCompany().getId())
                .stream().map(this::toColumnResponse).collect(Collectors.toList());
    }

    public CompanyDTO.ColumnResponse addColumn(String managerEmail, CompanyDTO.ColumnInput input) {
        User manager = getManagerOrThrow(managerEmail);
        Company company = companyRepository.findByManager(manager)
                .orElseThrow(() -> new ResourceNotFoundException("No company set up yet for this manager"));

        if (input.getFieldName() == null || input.getFieldName().isBlank()) {
            throw new IllegalArgumentException("Field name is required");
        }

        int nextIndex = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(company.getId()).size();

        TableColumn column = TableColumn.builder()
                .company(company)
                .fieldName(input.getFieldName())
                .fieldType(input.getFieldType() != null ? input.getFieldType() : FieldType.TEXT)
                .fieldRole(FieldRole.NONE)
                .isRequired(input.getIsRequired() != null && input.getIsRequired())
                .orderIndex(nextIndex)
                .description(input.getDescription())
                .defaultValue(input.getDefaultValue())
                .validationRules(input.getValidationRules())
                .options(input.getOptions())
                .build();

        TableColumn saved = tableColumnRepository.save(column);
        log.info("Column '{}' added to company {}", saved.getFieldName(), company.getId());
        return toColumnResponse(saved);
    }

    public void deleteColumn(String managerEmail, Long columnId) {
        User manager = getManagerOrThrow(managerEmail);
        Company company = companyRepository.findByManager(manager)
                .orElseThrow(() -> new ResourceNotFoundException("No company set up yet for this manager"));
        tableColumnRepository.deleteByCompanyIdAndId(company.getId(), columnId);
        log.info("Column {} deleted from company {}", columnId, company.getId());
    }

    public CompanyDTO.CompanyResponse updateEmployeeCredentials(
            String managerEmail, CompanyDTO.UpdateEmployeeCredentialsRequest request) {
        User manager = getManagerOrThrow(managerEmail);
        Company company = companyRepository.findByManager(manager)
                .orElseThrow(() -> new ResourceNotFoundException("No company set up yet for this manager"));

        if (request.getEmployeeUsername() != null && !request.getEmployeeUsername().isBlank()) {
            company.setEmployeeUsername(request.getEmployeeUsername());
        }
        if (request.getEmployeePassword() != null) {
            if (request.getEmployeePassword().length() < 4 || !request.getEmployeePassword().matches("\\d+")) {
                throw new IllegalArgumentException("Employee password must be 4 or more digits");
            }
            company.setEmployeePassword(request.getEmployeePassword());
        }

        Company saved = companyRepository.save(company);
        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(saved.getId());
        log.info("Employee credentials updated for company {}", saved.getId());
        return toCompanyResponse(saved, columns, "Employee credentials updated");
    }

    // ==================== HELPERS ====================

    private User getManagerOrThrow(String email) {
        User manager = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (manager.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only managers can perform this action");
        }
        if (manager.getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account not active. Awaiting payment approval.");
        }
        return manager;
    }

    private TableColumn buildSystemColumn(Company company, String fieldName, FieldType fieldType,
                                           FieldRole fieldRole, int orderIndex) {
        return TableColumn.builder()
                .company(company)
                .fieldName(fieldName)
                .fieldType(fieldType)
                .fieldRole(fieldRole)
                .isRequired(fieldRole != FieldRole.AMOUNT) // Amount is either auto-calculated or entered; never "required" to type past validation blocking it
                .orderIndex(orderIndex)
                .build();
    }

    private CompanyDTO.ColumnResponse toColumnResponse(TableColumn column) {
        return CompanyDTO.ColumnResponse.builder()
                .id(column.getId())
                .fieldName(column.getFieldName())
                .fieldType(column.getFieldType())
                .fieldRole(column.getFieldRole().toString())
                .isRequired(column.getIsRequired())
                .orderIndex(column.getOrderIndex())
                .description(column.getDescription())
                .defaultValue(column.getDefaultValue())
                .validationRules(column.getValidationRules())
                .options(column.getOptions())
                .build();
    }

    private CompanyDTO.CompanyResponse toCompanyResponse(Company company, List<TableColumn> columns, String message) {
        return CompanyDTO.CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .industry(company.getIndustry())
                .logoUrl(company.getLogoUrl())
                .description(company.getDescription())
                .tableDesign(company.getTableDesign())
                .status(company.getStatus().toString())
                .employeeUsername(company.getEmployeeUsername())
                .employeePassword(company.getEmployeePassword())
                .columns(columns.stream().map(this::toColumnResponse).collect(Collectors.toList()))
                .message(message)
                .build();
    }
}
