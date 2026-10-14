package com.frauddetection.engine;

import com.frauddetection.model.FraudRule;
import com.frauddetection.model.Transaction;
import com.frauddetection.repository.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RuleIntelligenceEngine {

    private final FraudRuleRepository ruleRepository;

    public RuleResult evaluate(Transaction transaction) {
        List<FraudRule> rules = ruleRepository.findByEnabledTrueOrderByWeightDesc();
        List<String> triggeredRules = new ArrayList<>();
        double ruleScore = 0.0;
        StringBuilder explanation = new StringBuilder();

        for (FraudRule rule : rules) {
            boolean triggered = evaluateRule(rule, transaction);
            if (triggered) {
                triggeredRules.add(rule.getName());
                ruleScore += rule.getRiskPoints() * rule.getWeight();
                explanation.append("- ").append(rule.getName()).append(": ").append(rule.getDescription()).append("\n");
            }
        }

        double normalizedScore = Math.min(ruleScore / 100.0, 1.0) * 100;
        double confidence = triggeredRules.isEmpty() ? 0.3 : Math.min(0.9, 0.5 + triggeredRules.size() * 0.1);

        return RuleResult.builder()
                .triggeredRules(triggeredRules)
                .ruleScore(normalizedScore)
                .confidence(confidence)
                .explanation(explanation.toString())
                .build();
    }

    private boolean evaluateRule(FraudRule rule, Transaction transaction) {
        return switch (rule.getCode()) {
            case "LARGE_AMOUNT" -> transaction.getAmount() != null &&
                    transaction.getAmount().compareTo(new BigDecimal(rule.getThreshold().longValue())) > 0;
            case "HIGH_RISK_COUNTRY" -> transaction.isHighRiskCountry();
            case "ROUND_AMOUNT" -> transaction.isRoundAmount();
            case "NEW_DEVICE" -> transaction.isNewDevice();
            case "FOREIGN_CURRENCY" -> transaction.isForeignCurrency();
            case "VPN_PROXY" -> transaction.isVpn();
            case "HIGH_RISK_MERCHANT" -> isHighRiskMerchant(transaction.getMerchantCategory());
            default -> false;
        };
    }

    private boolean isHighRiskMerchant(String category) {
        if (category == null) return false;
        return List.of("CRYPTO", "GAMBLING", "WIRE_TRANSFER", "MONEY_SERVICE").contains(category.toUpperCase());
    }

    public record RuleResult(
            List<String> triggeredRules,
            double ruleScore,
            double confidence,
            String explanation
    ) {
        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private List<String> triggeredRules;
            private double ruleScore;
            private double confidence;
            private String explanation;

            public Builder triggeredRules(List<String> v) { this.triggeredRules = v; return this; }
            public Builder ruleScore(double v) { this.ruleScore = v; return this; }
            public Builder confidence(double v) { this.confidence = v; return this; }
            public Builder explanation(String v) { this.explanation = v; return this; }
            public RuleResult build() { return new RuleResult(triggeredRules, ruleScore, confidence, explanation); }
        }
    }
}
