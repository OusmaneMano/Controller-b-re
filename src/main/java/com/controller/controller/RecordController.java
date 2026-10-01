package com.controller.controller;

import com.controller.dto.RecordDTO;
import com.controller.service.PdfExportService;
import com.controller.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;
    private final PdfExportService pdfExportService;

    @PostMapping({"/employee/records", "/manager/records"})
    public ResponseEntity<RecordDTO.RecordResponse> createRecord(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestBody RecordDTO.CreateRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createRecord(principal.getName(), companyId, request));
    }

    @GetMapping({"/employee/records", "/manager/records"})
    public ResponseEntity<RecordDTO.RecordListResponse> listRecords(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam Map<String, String> allParams) {
        allParams.remove("employeeId");
        allParams.remove("from");
        allParams.remove("to");
        return ResponseEntity.ok(recordService.listRecords(
                principal.getName(), companyId, employeeId, from, to, allParams));
    }

    @GetMapping({"/employee/records/export.pdf", "/manager/records/export.pdf"})
    public ResponseEntity<byte[]> exportPdf(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam Map<String, String> allParams) {
        allParams.remove("employeeId");
        allParams.remove("from");
        allParams.remove("to");
        byte[] pdf = pdfExportService.export(principal.getName(), companyId, employeeId, from, to, allParams);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=controller-records.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PutMapping({"/employee/records/{id}", "/manager/records/{id}"})
    public ResponseEntity<RecordDTO.RecordResponse> updateRecord(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @PathVariable Long id,
            @RequestBody RecordDTO.UpdateRecordRequest request) {
        return ResponseEntity.ok(recordService.updateRecord(principal.getName(), companyId, id, request));
    }

    @DeleteMapping({"/employee/records/{id}", "/manager/records/{id}"})
    public ResponseEntity<Void> deleteRecord(
            Principal principal,
            @RequestHeader(value = "X-Company-Id", required = false) Long companyId,
            @PathVariable Long id) {
        recordService.deleteRecord(principal.getName(), companyId, id);
        return ResponseEntity.noContent().build();
    }
}
