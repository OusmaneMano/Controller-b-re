package com.controller.service;

import com.controller.dto.RecordDTO;
import com.controller.entity.Company;
import com.controller.entity.Membership;
import com.controller.entity.Record;
import com.controller.entity.TableColumn;
import com.controller.entity.User;
import com.controller.entity.enums.FieldRole;
import com.controller.entity.enums.FieldType;
import com.controller.entity.enums.UserRole;
import com.controller.exception.ResourceNotFoundException;
import com.controller.repository.RecordRepository;
import com.controller.repository.TableColumnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
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
    private final MembershipService membershipService;

    public RecordDTO.RecordResponse createRecord(String email, Long companyId, RecordDTO.CreateRecordRequest request) {
        Membership m = membershipService.require(email, companyId);
        Company company = m.getCompany();
        User user = m.getUser();
        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(company.getId());
        Map<String, Object> data = request.getData() != null ? request.getData() : Map.of();
        Map<String, Object> processedData = applyAmountCalculation(columns, data);
        validateRequiredFields(columns, processedData);

        if (request.getClientKey() != null && !request.getClientKey().isBlank()) {
            Optional<Record> existing = recordRepository.findByCompanyIdAndCreatedAtBetween(
                            company.getId(), LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now().plusDays(1))
                    .stream()
                    .filter(r -> request.getClientKey().equals(r.getClientKey()))
                    .findFirst();
            if (existing.isPresent()) {
                return toRecordResponse(existing.get(), m.getRole());
            }
        }

        Record record = Record.builder()
                .company(company)
                .createdBy(user)
                .data(processedData)
                .clientKey(request.getClientKey())
                .deleted(false)
                .build();
        return toRecordResponse(recordRepository.save(record), m.getRole());
    }

    public RecordDTO.RecordResponse updateRecord(String email, Long companyId, Long recordId, RecordDTO.UpdateRecordRequest request) {
        Membership m = membershipService.require(email, companyId);
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        enforceSameCompany(m, record);
        enforceEditPermission(m, record);
        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(m.getCompany().getId());
        Map<String, Object> data = request.getData() != null ? request.getData() : Map.of();
        record.setData(applyAmountCalculation(columns, data));
        validateRequiredFields(columns, record.getData());
        return toRecordResponse(recordRepository.save(record), m.getRole());
    }

    public void deleteRecord(String email, Long companyId, Long recordId) {
        Membership m = membershipService.require(email, companyId);
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        enforceSameCompany(m, record);
        enforceEditPermission(m, record);
        record.setDeleted(true);
        record.setStatus("DELETED");
        recordRepository.save(record);
    }

    public RecordDTO.RecordListResponse listRecords(String email, Long companyId, Long employeeIdFilter,
                                                    LocalDateTime from, LocalDateTime to,
                                                    Map<String, String> rawParams) {
        Membership m = membershipService.require(email, companyId);
        List<Record> filtered = loadFiltered(m, employeeIdFilter, from, to, rawParams);
        return toListResponse(m, filtered);
    }

    List<Record> loadFiltered(Membership m, Long employeeIdFilter, LocalDateTime from, LocalDateTime to,
                              Map<String, String> rawParams) {
        Company company = m.getCompany();
        Long scopedEmployeeId = m.getRole() == UserRole.EMPLOYEE ? m.getUser().getId() : employeeIdFilter;
        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime effectiveTo = to != null ? to : LocalDateTime.now().plusMinutes(1);

        List<Record> records = (scopedEmployeeId != null)
                ? recordRepository.findByCompanyIdAndCreatedByIdAndCreatedAtBetween(
                company.getId(), scopedEmployeeId, effectiveFrom, effectiveTo)
                : recordRepository.findByCompanyIdAndCreatedAtBetween(company.getId(), effectiveFrom, effectiveTo);

        List<TableColumn> columns = tableColumnRepository.findByCompanyIdOrderByOrderIndexAsc(company.getId());
        return records.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getDeleted()))
                .filter(r -> matchesColumnFilters(r, columns, rawParams))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    RecordDTO.RecordListResponse toListResponse(Membership m, List<Record> records) {
        Optional<TableColumn> amountColumn = tableColumnRepository
                .findByCompanyIdAndFieldRole(m.getCompany().getId(), FieldRole.AMOUNT);
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
                .map(r -> toRecordResponse(r, m.getRole()))
                .collect(Collectors.toList());
        return RecordDTO.RecordListResponse.builder()
                .records(responses)
                .totalCount((long) responses.size())
                .totalAmount(totalAmount)
                .amountFieldName(amountColumn.map(TableColumn::getFieldName).orElse(null))
                .build();
    }

    /**
     * Column filters:
     * f.Product=rice          text contains
     * fmin.Amount=100         number >=
     * fmax.Amount=500         number <=
     */
    private boolean matchesColumnFilters(Record record, List<TableColumn> columns, Map<String, String> rawParams) {
        if (rawParams == null || rawParams.isEmpty()) return true;
        Map<String, Object> data = record.getData() != null ? record.getData() : Map.of();
        for (TableColumn col : columns) {
            String name = col.getFieldName();
            String eq = first(rawParams, "f." + name, "f_" + name);
            String min = first(rawParams, "fmin." + name, "fmin_" + name);
            String max = first(rawParams, "fmax." + name, "fmax_" + name);
            Object value = data.get(name);
            if (eq != null && !eq.isBlank()) {
                if (col.getFieldType() == FieldType.NUMBER) {
                    Double dv = toDouble(value);
                    Double want = toDouble(eq);
                    if (dv == null || want == null || Double.compare(dv, want) != 0) return false;
                } else {
                    if (value == null || !value.toString().toLowerCase(Locale.ROOT)
                            .contains(eq.toLowerCase(Locale.ROOT))) return false;
                }
            }
            if (min != null && !min.isBlank()) {
                Double dv = toDouble(value);
                Double want = toDouble(min);
                if (dv == null || want == null || dv < want) return false;
            }
            if (max != null && !max.isBlank()) {
                Double dv = toDouble(value);
                Double want = toDouble(max);
                if (dv == null || want == null || dv > want) return false;
            }
        }
        return true;
    }

    private String first(Map<String, String> params, String... keys) {
        for (String k : keys) {
            if (params.containsKey(k) && params.get(k) != null) return params.get(k);
        }
        return null;
    }

    private void enforceSameCompany(Membership m, Record record) {
        if (!m.getCompany().getId().equals(record.getCompany().getId())) {
            throw new IllegalArgumentException("This record does not belong to the selected company");
        }
    }

    private void enforceEditPermission(Membership m, Record record) {
        if (m.getRole() == UserRole.MANAGER) return;
        if (!record.getCreatedBy().getId().equals(m.getUser().getId())) {
            throw new IllegalArgumentException("You can only edit your own records");
        }
        if (!record.isEditable()) {
            throw new IllegalArgumentException("This record is locked. The 24-hour edit window has passed; only your manager can change it now.");
        }
    }

    private Map<String, Object> applyAmountCalculation(List<TableColumn> columns, Map<String, Object> data) {
        Optional<TableColumn> quantityCol = columns.stream().filter(c -> c.getFieldRole() == FieldRole.QUANTITY).findFirst();
        Optional<TableColumn> unitPriceCol = columns.stream().filter(c -> c.getFieldRole() == FieldRole.UNIT_PRICE).findFirst();
        Optional<TableColumn> amountCol = columns.stream().filter(c -> c.getFieldRole() == FieldRole.AMOUNT).findFirst();
        if (amountCol.isEmpty()) return data;
        java.util.Map<String, Object> result = new java.util.HashMap<>(data);
        if (quantityCol.isPresent() && unitPriceCol.isPresent()) {
            Double quantity = toDouble(data.get(quantityCol.get().getFieldName()));
            Double unitPrice = toDouble(data.get(unitPriceCol.get().getFieldName()));
            if (quantity == null || unitPrice == null) {
                throw new IllegalArgumentException("Quantity and Unit Price are required");
            }
            result.put(amountCol.get().getFieldName(), quantity * unitPrice);
        } else {
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
            return Double.parseDouble(value.toString().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RecordDTO.RecordResponse toRecordResponse(Record record, UserRole role) {
        return RecordDTO.RecordResponse.builder()
                .id(record.getId())
                .companyId(record.getCompany().getId())
                .createdById(record.getCreatedBy().getId())
                .createdByName(record.getCreatedBy().getFirstName() + " " + record.getCreatedBy().getLastName())
                .data(record.getData())
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .editable(role == UserRole.MANAGER || record.isEditable())
                .hoursRemaining(record.getHoursRemaining())
                .clientKey(record.getClientKey())
                .build();
    }
}
