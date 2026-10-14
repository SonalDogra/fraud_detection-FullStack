package com.frauddetection.service;

import com.frauddetection.ai.AiInvestigationService;
import com.frauddetection.engine.BehaviourIntelligenceEngine;
import com.frauddetection.engine.DeviceTrustEngine;
import com.frauddetection.engine.RiskAggregationEngine;
import com.frauddetection.engine.RuleIntelligenceEngine;
import com.frauddetection.model.Transaction;
import com.frauddetection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final RuleIntelligenceEngine ruleEngine;
    private final BehaviourIntelligenceEngine behaviourEngine;
    private final DeviceTrustEngine deviceTrustEngine;
    private final RiskAggregationEngine riskAggregationEngine;
    private final AiInvestigationService aiInvestigationService;

    @Transactional(readOnly = true)
    public Page<Transaction> findAll(String status, String riskLevel, String search, Pageable pageable) {
        Transaction.TransactionStatus statusEnum = null;
        Transaction.RiskLevel riskLevelEnum = null;

        if (status != null && !status.isEmpty()) {
            try { statusEnum = Transaction.TransactionStatus.valueOf(status); } catch (Exception ignored) {}
        }
        if (riskLevel != null && !riskLevel.isEmpty()) {
            try { riskLevelEnum = Transaction.RiskLevel.valueOf(riskLevel); } catch (Exception ignored) {}
        }

        return transactionRepository.findWithFilters(statusEnum, riskLevelEnum,
                (search != null && !search.isEmpty()) ? search : null, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Transaction> findByTransactionId(String txId) {
        return transactionRepository.findByTransactionId(txId);
    }

    public Transaction analyseTransaction(Transaction transaction) {
        log.info("Starting AI analysis pipeline for transaction: {}", transaction.getTransactionId());

        RuleIntelligenceEngine.RuleResult ruleResult = ruleEngine.evaluate(transaction);
        BehaviourIntelligenceEngine.BehaviourResult behaviourResult = behaviourEngine.analyse(transaction);
        DeviceTrustEngine.DeviceResult deviceResult = deviceTrustEngine.analyse(transaction);
        RiskAggregationEngine.AggregatedRisk aggregatedRisk = riskAggregationEngine.aggregate(
                ruleResult, behaviourResult, deviceResult);

        String aiReport = aiInvestigationService.generateInvestigationReport(
                transaction, ruleResult, behaviourResult, deviceResult, aggregatedRisk);

        transaction.setRiskScore(aggregatedRisk.finalScore());
        transaction.setConfidence(aggregatedRisk.confidence());
        transaction.setRiskLevel(aggregatedRisk.riskLevel());
        transaction.setTriggeredRules(String.join(", ", ruleResult.triggeredRules()));
        transaction.setBehaviourFindings(behaviourResult.explanation());
        transaction.setAiSummary(aiReport);
        transaction.setRecommendedAction(aggregatedRisk.recommendedAction());
        transaction.setAnalysedAt(LocalDateTime.now());

        transaction.setStatus(switch (aggregatedRisk.riskLevel()) {
            case CRITICAL -> Transaction.TransactionStatus.BLOCKED;
            case HIGH -> Transaction.TransactionStatus.FLAGGED;
            case MEDIUM -> Transaction.TransactionStatus.UNDER_REVIEW;
            case LOW -> Transaction.TransactionStatus.APPROVED;
        });

        return transactionRepository.save(transaction);
    }

    public Transaction save(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        long total = transactionRepository.count();
        long high = transactionRepository.countByRiskLevel(Transaction.RiskLevel.HIGH);
        long critical = transactionRepository.countByRiskLevel(Transaction.RiskLevel.CRITICAL);
        long fraud = transactionRepository.countByStatus(Transaction.TransactionStatus.FRAUD);
        long pending = transactionRepository.countByStatus(Transaction.TransactionStatus.PENDING);
        long approved = transactionRepository.countByStatus(Transaction.TransactionStatus.APPROVED);
        long blocked = transactionRepository.countByStatus(Transaction.TransactionStatus.BLOCKED);

        return new DashboardStats(total, high + critical, fraud, pending, approved, blocked,
                transactionRepository.findRecentHighRisk(Pageable.ofSize(5)),
                transactionRepository.countByRiskLevelGrouped(),
                transactionRepository.topFraudCategories());
    }

    public record DashboardStats(
            long total,
            long highRisk,
            long confirmedFraud,
            long pending,
            long approved,
            long blocked,
            java.util.List<Transaction> recentHighRisk,
            java.util.List<Object[]> riskDistribution,
            java.util.List<Object[]> topCategories
    ) {}
}
