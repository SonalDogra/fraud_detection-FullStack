package com.frauddetection.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transactionId;

    private String customerId;
    private String customerName;
    private String customerEmail;

    private String merchantName;
    private String merchantCategory;
    private String merchantId;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    private String currency;

    private String country;
    private String city;
    private String ipAddress;
    private String deviceId;
    private String deviceType;
    private String browser;
    private String os;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    private Integer riskScore;
    private Double confidence;

    private boolean isVpn;
    private boolean isNewDevice;
    private boolean isHighRiskCountry;
    private boolean isForeignCurrency;
    private boolean isRoundAmount;

    private LocalDateTime transactionTime;
    private LocalDateTime createdAt;
    private LocalDateTime analysedAt;

    @Lob
    private String aiSummary;

    @Lob
    private String triggeredRules;

    @Lob
    private String behaviourFindings;

    @Lob
    private String recommendedAction;

    public enum TransactionStatus {
        PENDING, APPROVED, FLAGGED, BLOCKED, UNDER_REVIEW, LEGITIMATE, FRAUD, FALSE_POSITIVE
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}
