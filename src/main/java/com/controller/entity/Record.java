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

    @Column(name = "client_key")
    private String clientKey;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean deleted;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (deleted == null) deleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public Boolean isEditable() {
        if (createdAt == null) return true;
        return LocalDateTime.now().isBefore(createdAt.plusHours(24));
    }

    @Transient
    public Long getHoursRemaining() {
        if (createdAt == null) return 24L;
        LocalDateTime editDeadline = createdAt.plusHours(24);
        if (LocalDateTime.now().isBefore(editDeadline)) {
            return java.time.temporal.ChronoUnit.HOURS.between(LocalDateTime.now(), editDeadline);
        }
        return 0L;
    }
}
