package com.frauddetection.repository;

import com.frauddetection.model.FraudCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, Long> {
    Optional<FraudCase> findByCaseId(String caseId);

    Page<FraudCase> findByStatusOrderByCreatedAtDesc(FraudCase.CaseStatus status, Pageable pageable);

    @Query("SELECT fc FROM FraudCase fc WHERE " +
           "(:status IS NULL OR fc.status = :status) AND " +
           "(:priority IS NULL OR fc.priority = :priority) AND " +
           "(:search IS NULL OR LOWER(fc.caseId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(fc.assignedTo) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY fc.createdAt DESC")
    Page<FraudCase> findWithFilters(
            @Param("status") FraudCase.CaseStatus status,
            @Param("priority") FraudCase.CasePriority priority,
            @Param("search") String search,
            Pageable pageable);

    long countByStatus(FraudCase.CaseStatus status);

    Optional<FraudCase> findByTransaction_TransactionId(String transactionId);
}
