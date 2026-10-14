package com.frauddetection.service;

import com.frauddetection.model.*;
import com.frauddetection.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSeedService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final FraudRuleRepository ruleRepository;
    private final FraudCaseRepository caseRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionService transactionService;

    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        seedUsers();
        seedRules();
        seedTransactions();
        log.info("Data seeding complete.");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        userRepository.saveAll(List.of(
            User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@fraudguard.ai")
                .fullName("System Administrator")
                .role(User.Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build(),
            User.builder()
                .username("analyst")
                .password(passwordEncoder.encode("analyst123"))
                .email("analyst@fraudguard.ai")
                .fullName("Sarah Chen")
                .role(User.Role.FRAUD_ANALYST)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build(),
            User.builder()
                .username("analyst2")
                .password(passwordEncoder.encode("analyst123"))
                .email("analyst2@fraudguard.ai")
                .fullName("James Patel")
                .role(User.Role.FRAUD_ANALYST)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build()
        ));
        log.info("Users seeded.");
    }

    private void seedRules() {
        if (ruleRepository.count() > 0) return;

        ruleRepository.saveAll(List.of(
            rule("Large Transaction", "LARGE_AMOUNT", "Transaction amount exceeds configured threshold", true, 1.0, 50000.0, 35, FraudRule.RuleCategory.AMOUNT),
            rule("High-Risk Country", "HIGH_RISK_COUNTRY", "Transaction originates from a high-risk jurisdiction", true, 0.9, 0.5, 30, FraudRule.RuleCategory.LOCATION),
            rule("Round Amount", "ROUND_AMOUNT", "Transaction amount is suspiciously round (e.g. 10000.00)", true, 0.6, 0.5, 15, FraudRule.RuleCategory.AMOUNT),
            rule("New Device", "NEW_DEVICE", "Transaction made from a device not previously seen", true, 0.8, 0.5, 25, FraudRule.RuleCategory.DEVICE),
            rule("Foreign Currency", "FOREIGN_CURRENCY", "Transaction made in a currency different from account's primary currency", true, 0.7, 0.5, 20, FraudRule.RuleCategory.AMOUNT),
            rule("VPN/Proxy Detected", "VPN_PROXY", "IP address associated with VPN or proxy service", true, 0.85, 0.5, 30, FraudRule.RuleCategory.DEVICE),
            rule("High-Risk Merchant", "HIGH_RISK_MERCHANT", "Merchant category is associated with elevated fraud risk", true, 0.75, 0.5, 25, FraudRule.RuleCategory.MERCHANT)
        ));
        log.info("Rules seeded.");
    }

    private FraudRule rule(String name, String code, String desc, boolean enabled, double weight, double threshold, int points, FraudRule.RuleCategory cat) {
        return FraudRule.builder()
                .name(name).code(code).description(desc).enabled(enabled)
                .weight(weight).threshold(threshold).riskPoints(points).category(cat)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private void seedTransactions() {
        if (transactionRepository.count() > 0) return;

        String[] customers = {"Priya Sharma", "James Okafor", "Liu Wei", "Maria Garcia", "Alex Thompson", "Fatima Al-Said", "Raj Patel", "Emma Wilson"};
        String[] merchants = {"Amazon", "Netflix", "Steam", "Apple Store", "Crypto Exchange", "International Wire", "Casino Royale", "Uber", "Swiggy", "Flipkart"};
        String[] categories = {"E_COMMERCE", "ENTERTAINMENT", "GAMING", "TECHNOLOGY", "CRYPTO", "WIRE_TRANSFER", "GAMBLING", "TRANSPORT", "FOOD", "RETAIL"};
        String[] countries = {"IN", "US", "GB", "NG", "CN", "AE", "RU", "BR", "KP", "IR"};
        String[] currencies = {"INR", "USD", "GBP", "EUR", "BTC"};
        String[] devices = {"Mobile", "Desktop", "Tablet", "Unknown"};

        for (int i = 0; i < 60; i++) {
            int mIdx = random.nextInt(merchants.length);
            int cIdx = random.nextInt(countries.length);
            boolean highRisk = cIdx >= 7;
            boolean vpn = random.nextDouble() < 0.2;
            boolean newDev = random.nextDouble() < 0.25;
            boolean foreign = !currencies[random.nextInt(currencies.length)].equals("INR");
            double amt = highRisk ? (50000 + random.nextDouble() * 200000) : (500 + random.nextDouble() * 10000);
            boolean round = amt % 1000 == 0;

            Transaction tx = Transaction.builder()
                    .transactionId("TXN-" + String.format("%06d", i + 1000))
                    .customerId("CUST-" + String.format("%04d", i % customers.length + 1))
                    .customerName(customers[i % customers.length])
                    .customerEmail((customers[i % customers.length].split(" ")[0].toLowerCase()) + "@email.com")
                    .merchantName(merchants[mIdx])
                    .merchantCategory(categories[mIdx])
                    .merchantId("MER-" + String.format("%04d", mIdx + 1))
                    .amount(BigDecimal.valueOf(Math.round(amt * 100.0) / 100.0))
                    .currency(currencies[random.nextInt(currencies.length)])
                    .country(countries[cIdx])
                    .city("City-" + cIdx)
                    .ipAddress("203." + (10 + cIdx) + ".45." + (100 + i % 50))
                    .deviceId("DEV-" + UUID.randomUUID().toString().substring(0, 8))
                    .deviceType(devices[random.nextInt(devices.length)])
                    .browser(random.nextBoolean() ? "Chrome" : "Safari")
                    .os(random.nextBoolean() ? "Android" : "iOS")
                    .isVpn(vpn)
                    .isNewDevice(newDev)
                    .isHighRiskCountry(highRisk)
                    .isForeignCurrency(foreign)
                    .isRoundAmount(round)
                    .status(Transaction.TransactionStatus.PENDING)
                    .transactionTime(LocalDateTime.now().minusHours(random.nextInt(72)))
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                transactionService.analyseTransaction(tx);
            } catch (Exception e) {
                log.warn("Failed to analyse transaction {}: {}", tx.getTransactionId(), e.getMessage());
                tx.setStatus(Transaction.TransactionStatus.PENDING);
                tx.setRiskScore(50);
                tx.setRiskLevel(Transaction.RiskLevel.MEDIUM);
                transactionRepository.save(tx);
            }
        }

        log.info("Transactions seeded and analysed.");

        seedCases();
    }

    private void seedCases() {
        List<Transaction> flagged = transactionRepository
                .findByStatusOrderByTransactionTimeDesc(Transaction.TransactionStatus.FLAGGED,
                        org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent();

        for (int i = 0; i < Math.min(flagged.size(), 5); i++) {
            Transaction tx = flagged.get(i);
            FraudCase.CasePriority priority = tx.getRiskLevel() == Transaction.RiskLevel.CRITICAL
                    ? FraudCase.CasePriority.CRITICAL : FraudCase.CasePriority.HIGH;

            FraudCase fraudCase = FraudCase.builder()
                    .caseId("CASE-" + String.format("%04d", i + 1))
                    .transaction(tx)
                    .status(i == 0 ? FraudCase.CaseStatus.RESOLVED_FRAUD : FraudCase.CaseStatus.IN_PROGRESS)
                    .priority(priority)
                    .createdBy("analyst")
                    .assignedTo(i % 2 == 0 ? "analyst" : "analyst2")
                    .notes("Initial review completed. Escalated for further investigation.")
                    .investigationSummary(tx.getAiSummary())
                    .createdAt(LocalDateTime.now().minusHours(i * 3))
                    .updatedAt(LocalDateTime.now())
                    .resolvedAt(i == 0 ? LocalDateTime.now() : null)
                    .build();

            caseRepository.save(fraudCase);
        }

        log.info("Cases seeded.");
    }
}
