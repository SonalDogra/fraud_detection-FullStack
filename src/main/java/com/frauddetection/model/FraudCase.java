package com.frauddetection.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    private CasePriority priority;

    private String assignedTo;
    private String createdBy;

    @Lob
    private String notes;

    @Lob
    private String investigationSummary;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public enum CaseStatus {
        OPEN, IN_PROGRESS, PENDING_REVIEW, RESOLVED_FRAUD, RESOLVED_LEGITIMATE, RESOLVED_FALSE_POSITIVE
    }

    public enum CasePriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
