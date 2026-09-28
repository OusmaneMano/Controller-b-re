package com.controller.service;

import com.controller.dto.RecordDTO;
import com.controller.entity.Company;
import com.controller.entity.Record;
import com.controller.entity.TableColumn;
import com.controller.entity.User;
import com.controller.entity.enums.FieldRole;
import com.controller.entity.enums.UserRole;
import com.controller.exception.ResourceNotFoundException;
import com.controller.repository.CompanyRepository;
import com.controller.repository.RecordRepository;
import com.controller.repository.TableColumnRepository;
import com.controller.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RecordService {

    private final RecordRepository recordRepository;
    private final TableColumnRepository tableColumnRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    // ==================== CREATE ====================

    public RecordDTO.RecordResponse createRecord(String employeeEmail, RecordDTO.CreateRecordRequest request) {
        User employee = getUserOrThrow(employeeEmail);
        Company company = getEmployeeCompanyOrThrow(employee);

        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(company.getId());
        Map<String, Object> data = request.getData() != null ? request.getData() : Map.of();
        Map<String, Object> processedData = applyAmountCalculation(columns, data);
        validateRequiredFields(columns, processedData);

        Record record = Record.builder()
                .company(company)
                .createdBy(employee)
                .data(processedData)
                .build();

        Record saved = recordRepository.save(record);
        log.info("Record {} created by {} for company {}", saved.getId(), employeeEmail, company.getId());
        return toRecordResponse(saved);
    }

    // ==================== UPDATE ====================

    public RecordDTO.RecordResponse updateRecord(String requesterEmail, Long recordId, RecordDTO.UpdateRecordRequest request) {
        User requester = getUserOrThrow(requesterEmail);
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        enforceEditPermission(requester, record);

        Company company = record.getCompany();
        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(company.getId());
        Map<String, Object> data = request.getData() != null ? request.getData() : Map.of();
        Map<String, Object> processedData = applyAmountCalculation(columns, data);
        validateRequiredFields(columns, processedData);

        record.setData(processedData);
        Record saved = recordRepository.save(record);
        log.info("Record {} updated by {}", saved.getId(), requesterEmail);
        return toRecordResponse(saved);
    }

    // ==================== DELETE ====================

    public void deleteRecord(String requesterEmail, Long recordId) {
        User requester = getUserOrThrow(requesterEmail);
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        enforceEditPermission(requester, record);
        recordRepository.delete(record);
        log.info("Record {} deleted by {}", recordId, requesterEmail);
    }

    // ==================== LIST + TOTAL AMOUNT ====================

    /**
     * Employee: sees only their own records within their company.
     * Manager: sees every record within their company, optionally filtered to one employee.
     * Both: optional date range filter. Total Amount is summed across every matching
     * record, not just what's returned, using whichever column has FieldRole.AMOUNT (if any).
     */
    public RecordDTO.RecordListResponse listRecords(String requesterEmail, Long employeeIdFilter,
                                                      LocalDateTime from, LocalDateTime to) {
        User requester = getUserOrThrow(requesterEmail);
        Company company;
        Long scopedEmployeeId;

        if (requester.getRole() == UserRole.EMPLOYEE) {
            company = getEmployeeCompanyOrThrow(requester);
            scopedEmployeeId = requester.getId(); // employees can only ever see their own records
        } else if (requester.getRole() == UserRole.MANAGER) {
            company = companyRepository.findByManager(requester)
                    .orElseThrow(() -> new ResourceNotFoundException("No company set up yet for this manager"));
            scopedEmployeeId = employeeIdFilter; // null = all employees
        } else {
            throw new IllegalArgumentException("Only managers and employees can list records");
        }

        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now();

        List<Record> records = (scopedEmployeeId != null)
                ? recordRepository.findByCompanyIdAndCreatedByIdAndCreatedAtBetween(
                        company.getId(), scopedEmployeeId, effectiveFrom, effectiveTo)
                : recordRepository.findByCompanyIdAndCreatedAtBetween(company.getId(), effectiveFrom, effectiveTo);

        Optional<TableColumn> amountColumn = tableColumnRepository
                .findByCompanyIdAndFieldRole(company.getId(), FieldRole.AMOUNT);

        Double totalAmount = null;
        if (amountColumn.isPresent()) {
            String amountField = amountColumn.get().getFieldName();
            totalAmount = records.stream()
                    .map(r -> toDouble(r.getData() != null ? r.getData().get(amountField) : null))
                    .filter(java.util.Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();
        }

        List<RecordDTO.RecordResponse> responses = records.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toRecordResponse)
                .collect(Collectors.toList());

        return RecordDTO.RecordListResponse.builder()
                .records(responses)
                .totalCount((long) responses.size())
                .totalAmount(totalAmount)
                .amountFieldName(amountColumn.map(TableColumn::getFieldName).orElse(null))
                .build();
    }

    // ==================== HELPERS ====================

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Company getEmployeeCompanyOrThrow(User employee) {
        if (employee.getCompany() == null) {
            throw new IllegalArgumentException("You are not linked to a company yet");
        }
        return employee.getCompany();
    }

    private void enforceEditPermission(User requester, Record record) {
        if (requester.getRole() == UserRole.MANAGER) {
            // Managers can always edit/delete records within their own company
            Company managed = companyRepository.findByManager(requester)
                    .orElseThrow(() -> new ResourceNotFoundException("No company set up yet for this manager"));
            if (!managed.getId().equals(record.getCompany().getId())) {
                throw new IllegalArgumentException("This record does not belong to your company");
            }
        } else if (requester.getRole() == UserRole.EMPLOYEE) {
            if (!record.getCreatedBy().getId().equals(requester.getId())) {
                throw new IllegalArgumentException("You can only edit your own records");
            }
            if (!record.isEditable()) {
                throw new IllegalArgumentException("This record is locked. The 24-hour edit window has passed; only your manager can change it now.");
            }
        } else {
            throw new IllegalArgumentException("You do not have permission to edit records");
        }
    }

    /**
     * If the company has Quantity + Unit Price columns, Amount is always
     * recalculated server-side (Quantity * Unit Price) - never trusted from the client.
     * If the company only has an Amount column, the client-provided value is kept as-is.
     */
    private Map<String, Object> applyAmountCalculation(List<TableColumn> columns, Map<String, Object> data) {
        Optional<TableColumn> quantityCol = columns.stream().filter(c -> c.getFieldRole() == FieldRole.QUANTITY).findFirst();
        Optional<TableColumn> unitPriceCol = columns.stream().filter(c -> c.getFieldRole() == FieldRole.UNIT_PRICE).findFirst();
        Optional<TableColumn> amountCol = columns.stream().filter(c -> c.getFieldRole() == FieldRole.AMOUNT).findFirst();

        if (amountCol.isEmpty()) {
            return data; // this company doesn't track Amount at all
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>(data);

        if (quantityCol.isPresent() && unitPriceCol.isPresent()) {
            Double quantity = toDouble(data.get(quantityCol.get().getFieldName()));
            Double unitPrice = toDouble(data.get(unitPriceCol.get().getFieldName()));
            if (quantity == null || unitPrice == null) {
                throw new IllegalArgumentException("Quantity and Unit Price are required");
            }
            result.put(amountCol.get().getFieldName(), quantity * unitPrice);
        } else {
            // Amount-only mode: keep whatever the employee typed, but validate it's numeric
            Double amount = toDouble(data.get(amountCol.get().getFieldName()));
            if (amount == null) {
                throw new IllegalArgumentException("Amount is required");
            }
            result.put(amountCol.get().getFieldName(), amount);
        }

        return result;
    }

    private void validateRequiredFields(List<TableColumn> columns, Map<String, Object> data) {
        for (TableColumn column : columns) {
            if (Boolean.TRUE.equals(column.getIsRequired())) {
                Object value = data.get(column.getFieldName());
                if (value == null || (value instanceof String s && s.isBlank())) {
                    throw new IllegalArgumentException("Field '" + column.getFieldName() + "' is required");
                }
            }
        }
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RecordDTO.RecordResponse toRecordResponse(Record record) {
        return RecordDTO.RecordResponse.builder()
                .id(record.getId())
                .companyId(record.getCompany().getId())
                .createdById(record.getCreatedBy().getId())
                .createdByName(record.getCreatedBy().getFirstName() + " " + record.getCreatedBy().getLastName())
                .data(record.getData())
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .editable(record.isEditable())
                .hoursRemaining(record.getHoursRemaining())
                .build();
    }
}
