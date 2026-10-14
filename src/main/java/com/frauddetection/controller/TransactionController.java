package com.frauddetection.controller;

import com.frauddetection.dto.NewTransactionForm;
import com.frauddetection.model.Transaction;
import com.frauddetection.service.AuditLogService;
import com.frauddetection.service.FraudCaseService;
import com.frauddetection.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final FraudCaseService caseService;
    private final AuditLogService auditLogService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String search,
            Model model) {

        Page<Transaction> transactions = transactionService.findAll(
                status, riskLevel, search,
                PageRequest.of(page, size, Sort.by("transactionTime").descending()));

        model.addAttribute("transactions", transactions);
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedRiskLevel", riskLevel);
        model.addAttribute("search", search);
        model.addAttribute("statuses", Transaction.TransactionStatus.values());
        model.addAttribute("riskLevels", Transaction.RiskLevel.values());
        model.addAttribute("pageTitle", "Transactions");
        model.addAttribute("activePage", "transactions");
        return "transactions/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "New Transaction");
        model.addAttribute("activePage", "transactions");
        return "transactions/new";
    }

    @PostMapping("/new")
    public String submit(@ModelAttribute NewTransactionForm form,
                         @AuthenticationPrincipal UserDetails user,
                         RedirectAttributes redirectAttributes) {
        try {
            String txId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Transaction tx = Transaction.builder()
                    .transactionId(txId)
                    .customerId(form.getCustomerId())
                    .customerName(form.getCustomerName())
                    .customerEmail(form.getCustomerEmail())
                    .merchantName(form.getMerchantName())
                    .merchantCategory(form.getMerchantCategory())
                    .merchantId(form.getMerchantId() != null ? form.getMerchantId() : "MER-MANUAL")
                    .amount(form.getAmount())
                    .currency(form.getCurrency())
                    .country(form.getCountry())
                    .city(form.getCity())
                    .ipAddress(form.getIpAddress() != null ? form.getIpAddress() : "0.0.0.0")
                    .deviceId("DEV-" + UUID.randomUUID().toString().substring(0, 8))
                    .deviceType(form.getDeviceType())
                    .browser(form.getBrowser())
                    .os(form.getOs())
                    .isVpn(form.isVpn())
                    .isNewDevice(form.isNewDevice())
                    .isHighRiskCountry(form.isHighRiskCountry())
                    .isForeignCurrency(form.isForeignCurrency())
                    .isRoundAmount(form.isRoundAmount())
                    .status(Transaction.TransactionStatus.PENDING)
                    .transactionTime(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            Transaction analysed = transactionService.analyseTransaction(tx);
            auditLogService.log(user.getUsername(), "SUBMIT_TRANSACTION", "Transaction", txId,
                    "Manual transaction submitted by " + user.getUsername());

            redirectAttributes.addFlashAttribute("success",
                    "Transaction " + txId + " analysed — Risk: " + analysed.getRiskLevel() + " (" + analysed.getRiskScore() + "/100)");
            return "redirect:/transactions/" + txId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to process transaction: " + e.getMessage());
            return "redirect:/transactions/new";
        }
    }

    @GetMapping("/{txId}")
    public String detail(@PathVariable String txId, Model model) {
        Transaction transaction = transactionService.findByTransactionId(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + txId));

        boolean hasCase = caseService.findByTransactionId(txId).isPresent();

        model.addAttribute("transaction", transaction);
        model.addAttribute("hasCase", hasCase);
        model.addAttribute("pageTitle", "Investigate — " + txId);
        model.addAttribute("activePage", "transactions");
        return "transactions/detail";
    }

    @PostMapping("/{txId}/open-case")
    public String openCase(@PathVariable String txId,
                           @AuthenticationPrincipal UserDetails user,
                           RedirectAttributes redirectAttributes) {
        try {
            var fraudCase = caseService.openCase(txId, user.getUsername());
            auditLogService.log(user.getUsername(), "OPEN_CASE", "FraudCase", fraudCase.getCaseId(),
                    "Case opened for transaction " + txId);
            redirectAttributes.addFlashAttribute("success", "Case " + fraudCase.getCaseId() + " opened successfully.");
            return "redirect:/cases/" + fraudCase.getCaseId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to open case: " + e.getMessage());
            return "redirect:/transactions/" + txId;
        }
    }
}
