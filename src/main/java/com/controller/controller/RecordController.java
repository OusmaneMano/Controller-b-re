package com.controller.controller;

import com.controller.dto.RecordDTO;
import com.controller.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RecordController {

    private final RecordService recordService;

    // ==================== EMPLOYEE ====================

    /**
     * POST /api/employee/records
     */
    @PostMapping("/employee/records")
    public ResponseEntity<RecordDTO.RecordResponse> createRecord(
            Principal principal, @RequestBody RecordDTO.CreateRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createRecord(principal.getName(), request));
    }

    /**
     * Employee's own records, filtered by date range if given.
     * Includes a Total Amount summed across every matching record, when the
     * company tracks an Amount field.
     * GET /api/employee/records?from=...&to=...
     */
    @GetMapping("/employee/records")
    public ResponseEntity<RecordDTO.RecordListResponse> listMyRecords(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(recordService.listRecords(principal.getName(), null, from, to));
    }

    /**
     * Employee can edit their own record within the 24-hour window.
     * PUT /api/employee/records/{id}
     */
    @PutMapping("/employee/records/{id}")
    public ResponseEntity<RecordDTO.RecordResponse> updateOwnRecord(
            Principal principal, @PathVariable Long id, @RequestBody RecordDTO.UpdateRecordRequest request) {
        return ResponseEntity.ok(recordService.updateRecord(principal.getName(), id, request));
    }

    /**
     * Employee can delete their own record within the 24-hour window.
     * DELETE /api/employee/records/{id}
     */
    @DeleteMapping("/employee/records/{id}")
    public ResponseEntity<Void> deleteOwnRecord(Principal principal, @PathVariable Long id) {
        recordService.deleteRecord(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    // ==================== MANAGER ====================

    /**
     * Every record in the manager's company, optionally filtered to one
     * employee and/or a date range. Includes the Total Amount across the
     * whole filtered set, not just what's shown on screen.
     * GET /api/manager/records?employeeId=...&from=...&to=...
     */
    @GetMapping("/manager/records")
    public ResponseEntity<RecordDTO.RecordListResponse> listCompanyRecords(
            Principal principal,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(recordService.listRecords(principal.getName(), employeeId, from, to));
    }

    /**
     * Managers can always edit any record in their own company, any time.
     * PUT /api/manager/records/{id}
     */
    @PutMapping("/manager/records/{id}")
    public ResponseEntity<RecordDTO.RecordResponse> updateAnyRecord(
            Principal principal, @PathVariable Long id, @RequestBody RecordDTO.UpdateRecordRequest request) {
        return ResponseEntity.ok(recordService.updateRecord(principal.getName(), id, request));
    }

    /**
     * Managers can always delete any record in their own company, any time.
     * DELETE /api/manager/records/{id}
     */
    @DeleteMapping("/manager/records/{id}")
    public ResponseEntity<Void> deleteAnyRecord(Principal principal, @PathVariable Long id) {
        recordService.deleteRecord(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
