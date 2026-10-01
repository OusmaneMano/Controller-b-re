package com.controller.entity;

import com.controller.entity.enums.CompanyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String industry;

    private String logoUrl;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(nullable = false)
    private String employeeUsername;

    @Column(nullable = false)
    private String employeePassword;

    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'PROFESSIONAL'")
    private String tableDesign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'ACTIVE'")
    private CompanyStatus status;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean demo;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (demo == null) demo = false;
        if (status == null) status = Boolean.TRUE.equals(demo) ? CompanyStatus.DEMO : CompanyStatus.ACTIVE;
        if (tableDesign == null || tableDesign.isBlank()) tableDesign = "PROFESSIONAL";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
