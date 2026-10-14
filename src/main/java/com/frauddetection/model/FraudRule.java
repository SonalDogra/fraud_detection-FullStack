package com.frauddetection.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Lob
    private String description;

    private boolean enabled;
    private Double weight;
    private Double threshold;
    private Integer riskPoints;

    @Enumerated(EnumType.STRING)
    private RuleCategory category;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum RuleCategory {
        AMOUNT, LOCATION, DEVICE, MERCHANT, VELOCITY, BEHAVIOUR, PATTERN
    }
}
