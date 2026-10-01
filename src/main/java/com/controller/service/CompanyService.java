package com.controller.service;

import com.controller.dto.CompanyDTO;
import com.controller.entity.Company;
import com.controller.entity.Membership;
import com.controller.entity.TableColumn;
import com.controller.entity.User;
import com.controller.entity.enums.CompanyStatus;
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
    private final MembershipService membershipService;

    public CompanyDTO.CompanyResponse setupCompany(String managerEmail, CompanyDTO.SetupRequest request) {
        User manager = userRepository.findByEmail(managerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (manager.getUserStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Your payment is not approved yet. Explore the demo, send payment, then wait for approval before creating your own table.");
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
        if (companyRepository.findByEmployeeUsername(request.getEmployeeUsername()).isPresent()) {
            throw new IllegalArgumentException("That employee username is already used by another company");
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
                .status(CompanyStatus.ACTIVE)
                .demo(false)
                .build();

        Company savedCompany = companyRepository.save(company);
        membershipService.add(manager, savedCompany, UserRole.MANAGER);
        manager.setLastCompanyId(savedCompany.getId());
        manager.setCompany(savedCompany);
        userRepository.save(manager);

        int orderIndex = 0;
        List<TableColumn> columns = new ArrayList<>();
        CompanyDTO.AmountColumnsOption option = request.getAmountColumnsOption();
        if (option == CompanyDTO.AmountColumnsOption.QUANTITY_UNIT_PRICE_AMOUNT) {
            columns.add(buildSystemColumn(savedCompany, "Quantity", FieldType.NUMBER, FieldRole.QUANTITY, orderIndex++));
            columns.add(buildSystemColumn(savedCompany, "Unit Price", FieldType.NUMBER, FieldRole.UNIT_PRICE, orderIndex++));
            columns.add(buildSystemColumn(savedCompany, "Amount", FieldType.NUMBER, FieldRole.AMOUNT, orderIndex++));
        } else if (option == CompanyDTO.AmountColumnsOption.AMOUNT_ONLY) {
            columns.add(buildSystemColumn(savedCompany, "Amount", FieldType.NUMBER, FieldRole.AMOUNT, orderIndex++));
        }

        if (request.getCustomColumns() != null) {
            for (CompanyDTO.ColumnInput input : request.getCustomColumns()) {
                if (input.getFieldName() == null || input.getFieldName().isBlank()) continue;
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
        return toCompanyResponse(savedCompany, savedColumns, UserRole.MANAGER, "Company set up successfully. Share the employee username and PIN with your staff.");
    }

    public CompanyDTO.CompanyResponse getMyCompany(String email, Long companyId) {
        Membership m = membershipService.require(email, companyId);
        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(m.getCompany().getId());
        return toCompanyResponse(m.getCompany(), columns, m.getRole(), null);
    }

    public List<CompanyDTO.ColumnResponse> getColumnsForEmployee(String email, Long companyId) {
        Membership m = membershipService.require(email, companyId);
        return tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(m.getCompany().getId())
                .stream().map(this::toColumnResponse).collect(Collectors.toList());
    }

    public CompanyDTO.ColumnResponse addColumn(String email, Long companyId, CompanyDTO.ColumnInput input) {
        Membership m = membershipService.require(email, companyId);
        if (m.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only the shop manager can add columns");
        }
        if (Boolean.TRUE.equals(m.getCompany().getDemo())) {
            throw new IllegalArgumentException("The demo shop columns are fixed");
        }
        if (input.getFieldName() == null || input.getFieldName().isBlank()) {
            throw new IllegalArgumentException("Field name is required");
        }
        int nextIndex = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(m.getCompany().getId()).size();
        TableColumn saved = tableColumnRepository.save(TableColumn.builder()
                .company(m.getCompany())
                .fieldName(input.getFieldName())
                .fieldType(input.getFieldType() != null ? input.getFieldType() : FieldType.TEXT)
                .fieldRole(FieldRole.NONE)
                .isRequired(input.getIsRequired() != null && input.getIsRequired())
                .orderIndex(nextIndex)
                .description(input.getDescription())
                .defaultValue(input.getDefaultValue())
                .validationRules(input.getValidationRules())
                .options(input.getOptions())
                .build());
        return toColumnResponse(saved);
    }

    public void deleteColumn(String email, Long companyId, Long columnId) {
        Membership m = membershipService.require(email, companyId);
        if (m.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only the shop manager can delete columns");
        }
        if (Boolean.TRUE.equals(m.getCompany().getDemo())) {
            throw new IllegalArgumentException("The demo shop columns are fixed");
        }
        tableColumnRepository.deleteByCompanyIdAndId(m.getCompany().getId(), columnId);
    }

    public CompanyDTO.CompanyResponse updateEmployeeCredentials(
            String email, Long companyId, CompanyDTO.UpdateEmployeeCredentialsRequest request) {
        Membership m = membershipService.require(email, companyId);
        if (m.getRole() != UserRole.MANAGER) {
            throw new IllegalArgumentException("Only the shop manager can change the staff PIN");
        }
        Company company = m.getCompany();
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
        return toCompanyResponse(saved, columns, UserRole.MANAGER, "Employee credentials updated");
    }

    private TableColumn buildSystemColumn(Company company, String fieldName, FieldType fieldType,
                                          FieldRole fieldRole, int orderIndex) {
        return TableColumn.builder()
                .company(company)
                .fieldName(fieldName)
                .fieldType(fieldType)
                .fieldRole(fieldRole)
                .isRequired(fieldRole != FieldRole.AMOUNT)
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

    private CompanyDTO.CompanyResponse toCompanyResponse(Company company, List<TableColumn> columns,
                                                         UserRole role, String message) {
        boolean managerView = role == UserRole.MANAGER;
        return CompanyDTO.CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .industry(company.getIndustry())
                .logoUrl(company.getLogoUrl())
                .description(company.getDescription())
                .tableDesign(company.getTableDesign())
                .status(company.getStatus().toString())
                .employeeUsername(managerView ? company.getEmployeeUsername() : null)
                .employeePassword(managerView ? company.getEmployeePassword() : null)
                .columns(columns.stream().map(this::toColumnResponse).collect(Collectors.toList()))
                .message(message)
                .build();
    }
}
