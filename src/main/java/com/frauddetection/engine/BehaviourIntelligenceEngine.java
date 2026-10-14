package com.frauddetection.engine;

import com.frauddetection.model.Transaction;
import com.frauddetection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BehaviourIntelligenceEngine {

    private final TransactionRepository transactionRepository;

    public BehaviourResult analyse(Transaction transaction) {
        List<Transaction> history = transactionRepository.findByRiskLevelOrderByTransactionTimeDesc(
                Transaction.RiskLevel.LOW, PageRequest.of(0, 50)).getContent();

        if (history.isEmpty()) {
            return buildResult(50.0, 0.4, "Insufficient transaction history for behaviour analysis.");
        }

        BigDecimal avgAmount = history.stream()
                .map(Transaction::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(Math.max(1, history.size())), 2, RoundingMode.HALF_UP);

        double behaviourScore = 20.0;
        StringBuilder explanation = new StringBuilder();

        if (transaction.getAmount() != null && avgAmount.compareTo(BigDecimal.ZERO) > 0) {
            double ratio = transaction.getAmount().doubleValue() / avgAmount.doubleValue();
            if (ratio > 10) {
                behaviourScore += 50;
                explanation.append(String.format(
                    "Transaction amount is %.0fx higher than the customer's average of %s. ", ratio, avgAmount));
            } else if (ratio > 5) {
                behaviourScore += 30;
                explanation.append(String.format(
                    "Transaction amount is %.0fx higher than average. ", ratio));
            } else if (ratio > 2) {
                behaviourScore += 15;
                explanation.append("Transaction amount is moderately above average. ");
            } else {
                explanation.append("Transaction amount is consistent with spending patterns. ");
            }
        }

        long sameCountry = history.stream()
                .filter(t -> transaction.getCountry() != null && transaction.getCountry().equals(t.getCountry()))
                .count();
        if (sameCountry == 0 && transaction.getCountry() != null) {
            behaviourScore += 20;
            explanation.append("Transaction originates from an unfamiliar country. ");
        }

        double confidence = Math.min(0.85, 0.5 + history.size() * 0.007);
        return buildResult(Math.min(behaviourScore, 100), confidence, explanation.toString());
    }

    private BehaviourResult buildResult(double score, double confidence, String explanation) {
        return new BehaviourResult(score, confidence, explanation);
    }

    public record BehaviourResult(double behaviourScore, double confidence, String explanation) {}
}
