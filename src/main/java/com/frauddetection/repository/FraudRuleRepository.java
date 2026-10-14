package com.frauddetection.repository;

import com.frauddetection.model.FraudRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, Long> {
    List<FraudRule> findByEnabledTrueOrderByWeightDesc();
    List<FraudRule> findByCategory(FraudRule.RuleCategory category);
    boolean existsByCode(String code);
}
