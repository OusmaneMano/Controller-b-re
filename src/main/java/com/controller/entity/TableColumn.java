package com.controller.entity;

import com.controller.entity.enums.FieldType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "table_columns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isRequired;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer orderIndex;

    private String description;

    private String defaultValue;

    @Type(JsonBinaryType.class)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    private Map<String, Object> validationRules;

    @Type(JsonBinaryType.class)
    @Column(name = "options", columnDefinition = "jsonb")
    private Map<String, Object> options;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isRequired == null) isRequired = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
