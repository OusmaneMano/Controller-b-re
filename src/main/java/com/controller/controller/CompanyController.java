package com.controller.controller;

import com.controller.dto.CompanyDTO;
import com.controller.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyService companyService;

    // ==================== MANAGER ====================

    /**
     * Full company setup in one call (Steps 5-8: company info, the Quantity/Unit
     * Price/Amount choice + custom columns, table design, employee credentials).
     * POST /api/manager/company/setup
     */
    @PostMapping("/manager/company/setup")
    public ResponseEntity<CompanyDTO.CompanyResponse> setupCompany(
            Principal principal, @RequestBody CompanyDTO.SetupRequest request) {
        CompanyDTO.CompanyResponse response = companyService.setupCompany(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/manager/company
     */
    @GetMapping("/manager/company")
    public ResponseEntity<CompanyDTO.CompanyResponse> getMyCompany(Principal principal) {
        return ResponseEntity.ok(companyService.getMyCompany(principal.getName()));
    }

    /**
     * Add one more column after initial setup (manager can edit anytime).
     * POST /api/manager/company/columns
     */
    @PostMapping("/manager/company/columns")
    public ResponseEntity<CompanyDTO.ColumnResponse> addColumn(
            Principal principal, @RequestBody CompanyDTO.AddColumnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.addColumn(principal.getName(), request.getColumn()));
    }

    /**
     * Delete a column (manager can edit anytime).
     * DELETE /api/manager/company/columns/{columnId}
     */
    @DeleteMapping("/manager/company/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(Principal principal, @PathVariable Long columnId) {
        companyService.deleteColumn(principal.getName(), columnId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manager can change the shared employee username/password anytime.
     * PUT /api/manager/company/employee-credentials
     */
    @PutMapping("/manager/company/employee-credentials")
    public ResponseEntity<CompanyDTO.CompanyResponse> updateEmployeeCredentials(
            Principal principal, @RequestBody CompanyDTO.UpdateEmployeeCredentialsRequest request) {
        return ResponseEntity.ok(companyService.updateEmployeeCredentials(principal.getName(), request));
    }

    // ==================== EMPLOYEE ====================

    /**
     * The columns an employee needs to fill in for their company's record form.
     * GET /api/employee/company/columns
     */
    @GetMapping("/employee/company/columns")
    public ResponseEntity<List<CompanyDTO.ColumnResponse>> getColumnsForEmployee(Principal principal) {
        return ResponseEntity.ok(companyService.getColumnsForEmployee(principal.getName()));
    }
}
