package com.frauddetection.engine;

import com.frauddetection.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class RiskAggregationEngine {

    private static final double RULE_WEIGHT = 0.35;
    private static final double BEHAVIOUR_WEIGHT = 0.30;
    private static final double DEVICE_WEIGHT = 0.20;
    private static final double SIMILARITY_WEIGHT = 0.15;

    public AggregatedRisk aggregate(
            RuleIntelligenceEngine.RuleResult ruleResult,
            BehaviourIntelligenceEngine.BehaviourResult behaviourResult,
            DeviceTrustEngine.DeviceResult deviceResult) {

        double weightedScore =
                (ruleResult.ruleScore() * RULE_WEIGHT) +
                (behaviourResult.behaviourScore() * BEHAVIOUR_WEIGHT) +
                (deviceResult.riskScore() * DEVICE_WEIGHT);

        double finalScore = Math.min(weightedScore, 100);

        double confidence = (ruleResult.confidence() + behaviourResult.confidence() + deviceResult.confidence()) / 3.0;

        Transaction.RiskLevel riskLevel = getRiskLevel(finalScore);
        String recommendedAction = getRecommendedAction(finalScore, riskLevel);

        String breakdown = String.format(
            "Rule Engine (35%%): %.1f | Behaviour Engine (30%%): %.1f | Device Engine (20%%): %.1f",
            ruleResult.ruleScore(), behaviourResult.behaviourScore(), deviceResult.riskScore()
        );

        return new AggregatedRisk(
            (int) Math.round(finalScore),
            confidence,
            riskLevel,
            recommendedAction,
            breakdown
        );
    }

    private Transaction.RiskLevel getRiskLevel(double score) {
        if (score >= 80) return Transaction.RiskLevel.CRITICAL;
        if (score >= 60) return Transaction.RiskLevel.HIGH;
        if (score >= 35) return Transaction.RiskLevel.MEDIUM;
        return Transaction.RiskLevel.LOW;
    }

    private String getRecommendedAction(double score, Transaction.RiskLevel level) {
        return switch (level) {
            case CRITICAL -> "BLOCK_TRANSACTION — Risk score critical. Immediate block required. Escalate to fraud team.";
            case HIGH -> "MANUAL_REVIEW — High risk detected. Place on temporary hold and assign to fraud analyst.";
            case MEDIUM -> "REQUIRE_OTP — Moderate risk. Request additional authentication from customer.";
            case LOW -> "APPROVE_WITH_MONITORING — Low risk. Approve and continue monitoring.";
        };
    }

    public record AggregatedRisk(
        int finalScore,
        double confidence,
        Transaction.RiskLevel riskLevel,
        String recommendedAction,
        String scoreBreakdown
    ) {}
}
