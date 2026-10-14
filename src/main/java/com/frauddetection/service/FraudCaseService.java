package com.frauddetection.service;

import com.frauddetection.model.FraudCase;
import com.frauddetection.model.Transaction;
import com.frauddetection.repository.FraudCaseRepository;
import com.frauddetection.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FraudCaseService {

    private final FraudCaseRepository caseRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Page<FraudCase> findAll(String status, String priority, String search, Pageable pageable) {
        FraudCase.CaseStatus statusEnum = null;
        FraudCase.CasePriority priorityEnum = null;

        if (status != null && !status.isEmpty()) {
            try { statusEnum = FraudCase.CaseStatus.valueOf(status); } catch (Exception ignored) {}
        }
        if (priority != null && !priority.isEmpty()) {
            try { priorityEnum = FraudCase.CasePriority.valueOf(priority); } catch (Exception ignored) {}
        }

        return caseRepository.findWithFilters(statusEnum, priorityEnum,
                (search != null && !search.isEmpty()) ? search : null, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<FraudCase> findByCaseId(String caseId) {
        return caseRepository.findByCaseId(caseId);
    }

    @Transactional(readOnly = true)
    public Optional<FraudCase> findByTransactionId(String transactionId) {
        return caseRepository.findByTransaction_TransactionId(transactionId);
    }

    public FraudCase openCase(String transactionId, String analystUsername) {
        Transaction tx = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        Optional<FraudCase> existing = caseRepository.findByTransaction_TransactionId(transactionId);
        if (existing.isPresent()) return existing.get();

        FraudCase.CasePriority priority = switch (tx.getRiskLevel()) {
            case CRITICAL -> FraudCase.CasePriority.CRITICAL;
            case HIGH -> FraudCase.CasePriority.HIGH;
            case MEDIUM -> FraudCase.CasePriority.MEDIUM;
            default -> FraudCase.CasePriority.LOW;
        };

        FraudCase fraudCase = FraudCase.builder()
                .caseId("CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .transaction(tx)
                .status(FraudCase.CaseStatus.OPEN)
                .priority(priority)
                .createdBy(analystUsername)
                .assignedTo(analystUsername)
                .investigationSummary(tx.getAiSummary())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return caseRepository.save(fraudCase);
    }

    public FraudCase updateCase(String caseId, String outcome, String notes, String analystUsername) {
        FraudCase fraudCase = caseRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        fraudCase.setNotes(notes);
        fraudCase.setUpdatedAt(LocalDateTime.now());

        FraudCase.CaseStatus newStatus = switch (outcome) {
            case "FRAUD" -> FraudCase.CaseStatus.RESOLVED_FRAUD;
            case "LEGITIMATE" -> FraudCase.CaseStatus.RESOLVED_LEGITIMATE;
            case "FALSE_POSITIVE" -> FraudCase.CaseStatus.RESOLVED_FALSE_POSITIVE;
            case "IN_PROGRESS" -> FraudCase.CaseStatus.IN_PROGRESS;
            default -> fraudCase.getStatus();
        };

        fraudCase.setStatus(newStatus);

        if (newStatus.name().startsWith("RESOLVED")) {
            fraudCase.setResolvedAt(LocalDateTime.now());
            Transaction.TransactionStatus txStatus = switch (outcome) {
                case "FRAUD" -> Transaction.TransactionStatus.FRAUD;
                case "LEGITIMATE" -> Transaction.TransactionStatus.LEGITIMATE;
                case "FALSE_POSITIVE" -> Transaction.TransactionStatus.FALSE_POSITIVE;
                default -> fraudCase.getTransaction().getStatus();
            };
            Transaction tx = fraudCase.getTransaction();
            tx.setStatus(txStatus);
            transactionRepository.save(tx);
        }

        return caseRepository.save(fraudCase);
    }

    @Transactional(readOnly = true)
    public CaseStats getStats() {
        long open = caseRepository.countByStatus(FraudCase.CaseStatus.OPEN);
        long inProgress = caseRepository.countByStatus(FraudCase.CaseStatus.IN_PROGRESS);
        long resolvedFraud = caseRepository.countByStatus(FraudCase.CaseStatus.RESOLVED_FRAUD);
        long resolvedLegit = caseRepository.countByStatus(FraudCase.CaseStatus.RESOLVED_LEGITIMATE);
        long falsePosive = caseRepository.countByStatus(FraudCase.CaseStatus.RESOLVED_FALSE_POSITIVE);
        return new CaseStats(open, inProgress, resolvedFraud, resolvedLegit, falsePosive);
    }

    public record CaseStats(long open, long inProgress, long resolvedFraud, long resolvedLegitimate, long falsePositive) {}
}
