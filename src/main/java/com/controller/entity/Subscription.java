package com.controller.entity;

import com.controller.entity.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier;

    @Column(nullable = false)
    private Double setupFee;

    @Column(nullable = false)
    private Double monthlyFee;

    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'PENDING'")
    private String status;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime renewalDate;

    @Column(nullable = false)
    private LocalDateTime paidUntil;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private Integer totalEmployees;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean paymentWarningSent;

    // Special Offers
    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean trialActive;

    @Column(name = "trial_expires")
    private LocalDateTime trialExpires;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean earlyBirdUsed;

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double earlyBirdDiscount;

    @Column(columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean annualPlanActive;

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double annualPlanPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_id")
    private Company referredBy;

    @Column(columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double referralCredits;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = "PENDING";
        startDate = LocalDateTime.now();
        renewalDate = LocalDateTime.now().plusMonths(1);
        paidUntil = LocalDateTime.now().plusMonths(1);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}