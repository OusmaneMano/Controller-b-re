package com.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecordDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRecordRequest {
        private Map<String, Object> data;
        private String clientKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRecordRequest {
        private Map<String, Object> data;
        private String clientKey;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordResponse {
        private Long id;
        private Long companyId;
        private Long createdById;
        private String createdByName;
        private Map<String, Object> data;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Boolean editable;
        private Long hoursRemaining;
        private String clientKey;
    }

    /**
     * Filtered record list, with Total Amount computed across every matching
     * record (not just the current page) whenever the company has an Amount column.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecordListResponse {
        private List<RecordResponse> records;
        private Long totalCount;
        private Double totalAmount;
        private String amountFieldName;
    }
}
