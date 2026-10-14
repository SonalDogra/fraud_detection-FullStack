package com.frauddetection.repository;

import com.frauddetection.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByOrderByTimestampDesc(Pageable pageable);
    Page<AuditLog> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);
}
