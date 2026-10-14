package com.frauddetection.repository;

import com.frauddetection.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByStatusOrderByTransactionTimeDesc(Transaction.TransactionStatus status, Pageable pageable);

    Page<Transaction> findByRiskLevelOrderByTransactionTimeDesc(Transaction.RiskLevel riskLevel, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:riskLevel IS NULL OR t.riskLevel = :riskLevel) AND " +
           "(:search IS NULL OR LOWER(t.transactionId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(t.merchantName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY t.transactionTime DESC")
    Page<Transaction> findWithFilters(
            @Param("status") Transaction.TransactionStatus status,
            @Param("riskLevel") Transaction.RiskLevel riskLevel,
            @Param("search") String search,
            Pageable pageable);

    long countByRiskLevel(Transaction.RiskLevel riskLevel);
    long countByStatus(Transaction.TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionTime >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT t.riskLevel, COUNT(t) FROM Transaction t GROUP BY t.riskLevel")
    List<Object[]> countByRiskLevelGrouped();

    @Query("SELECT t FROM Transaction t WHERE t.riskLevel IN ('HIGH', 'CRITICAL') ORDER BY t.transactionTime DESC")
    List<Transaction> findRecentHighRisk(Pageable pageable);

    @Query("SELECT t.merchantCategory, COUNT(t) FROM Transaction t WHERE t.riskLevel IN ('HIGH', 'CRITICAL') GROUP BY t.merchantCategory ORDER BY COUNT(t) DESC")
    List<Object[]> topFraudCategories();

    List<Transaction> findTop10ByOrderByTransactionTimeDesc();
}
