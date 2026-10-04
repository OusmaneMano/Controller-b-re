package com.controller.controller;

import com.controller.dto.CompanyDTO;
import com.controller.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/manager/company/setup")
    public ResponseEntity<CompanyDTO.CompanyResponse> setupCompany(
            Principal principal, @RequestBody CompanyDTO.SetupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.setupCompany(principal.getName(), request));
    }

    @GetMapping({"/manager/company", "/employee/company"})
    public ResponseEntity<CompanyDTO.CompanyResponse> getMyCompany(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        return ResponseEntity.ok(companyService.getMyCompany(principal.getName(), companyId));
    }

    @PostMapping("/manager/company/columns")
    public ResponseEntity<CompanyDTO.ColumnResponse> addColumn(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestBody CompanyDTO.AddColumnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.addColumn(principal.getName(), companyId, request.getColumn()));
    }

    @PutMapping("/manager/company/columns/{columnId}")
    public ResponseEntity<CompanyDTO.ColumnResponse> renameColumn(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @PathVariable Long columnId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(companyService.renameColumn(principal.getName(), companyId, columnId, body.get("fieldName")));
    }

    @PutMapping("/manager/company/columns/order")
    public ResponseEntity<Void> reorder(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestBody List<Long> columnIds) {
        companyService.reorderColumns(principal.getName(), companyId, columnIds);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/manager/company/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @PathVariable Long columnId) {
        companyService.deleteColumn(principal.getName(), companyId, columnId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/manager/company/employee-credentials")
    public ResponseEntity<CompanyDTO.CompanyResponse> updateEmployeeCredentials(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestBody CompanyDTO.UpdateEmployeeCredentialsRequest request) {
        return ResponseEntity.ok(companyService.updateEmployeeCredentials(principal.getName(), companyId, request));
    }

    @GetMapping("/employee/company/columns")
    public ResponseEntity<List<CompanyDTO.ColumnResponse>> getColumnsForEmployee(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId) {
        return ResponseEntity.ok(companyService.getColumnsForEmployee(principal.getName(), companyId));
    }
}