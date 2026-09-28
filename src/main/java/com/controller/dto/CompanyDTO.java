package com.controller.dto;

import com.controller.entity.enums.FieldType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyDTO {

    /**
     * The manager's answer to:
     * "Do you need Quantity, Unit Price and Amount columns, or do you only
     *  need the Amount column?"
     */
    public enum AmountColumnsOption {
        QUANTITY_UNIT_PRICE_AMOUNT,
        AMOUNT_ONLY,
        NONE
    }

    /**
     * One custom column the manager defines manually, in addition to
     * whichever Quantity/Unit Price/Amount columns amountColumnsOption adds.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInput {
        private String fieldName;
        private FieldType fieldType;
        private Boolean isRequired;
        private String description;
        private String defaultValue;
        private Map<String, Object> validationRules;
        private Map<String, Object> options;
    }

    /**
     * Full company setup request: Steps 5-8 of onboarding combined into one call
     * (company info, amount columns choice + custom columns, table design, employee credentials).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetupRequest {
        private String companyName;
        private String industry;
        private String logoUrl;
        private String description;

        private AmountColumnsOption amountColumnsOption;
        private List<ColumnInput> customColumns;

        private String tableDesign;

        private String employeeUsername;
        private String employeePassword;
    }

    /**
     * Request to add one more column after initial setup.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddColumnRequest {
        private ColumnInput column;
    }

    /**
     * Manager changing the shared employee username/password later.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEmployeeCredentialsRequest {
        private String employeeUsername;
        private String employeePassword;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnResponse {
        private Long id;
        private String fieldName;
        private FieldType fieldType;
        private String fieldRole;
        private Boolean isRequired;
        private Integer orderIndex;
        private String description;
        private String defaultValue;
        private Map<String, Object> validationRules;
        private Map<String, Object> options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompanyResponse {
        private Long id;
        private String name;
        private String industry;
        private String logoUrl;
        private String description;
        private String tableDesign;
        private String status;
        private String employeeUsername;
        private String employeePassword;
        private List<ColumnResponse> columns;
        private String message;
    }
}
