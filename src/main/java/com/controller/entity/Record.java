package com.controller.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Type(JsonBinaryType.class)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data;

    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'ACTIVE'")
    private String status;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public Boolean isEditable() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime editDeadline = createdAt.plusHours(24);
        return now.isBefore(editDeadline);
    }

    @Transient
    public Long getHoursRemaining() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime editDeadline = createdAt.plusHours(24);
        if (now.isBefore(editDeadline)) {
            return java.time.temporal.ChronoUnit.HOURS.between(now, editDeadline);
        }
        return 0L;
    }
}
