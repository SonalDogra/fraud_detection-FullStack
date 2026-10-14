package com.frauddetection.controller;

import com.frauddetection.model.Transaction;
import com.frauddetection.service.AuditLogService;
import com.frauddetection.service.PdfReportService;
import com.frauddetection.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final TransactionService transactionService;
    private final PdfReportService pdfReportService;
    private final AuditLogService auditLogService;

    @GetMapping("/transaction/{txId}/pdf")
    public ResponseEntity<byte[]> downloadTransactionReport(
            @PathVariable String txId,
            @AuthenticationPrincipal UserDetails user) {

        Transaction tx = transactionService.findByTransactionId(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + txId));

        try {
            byte[] pdf = pdfReportService.generateReport(tx);

            auditLogService.log(user.getUsername(), "DOWNLOAD_REPORT", "Transaction", txId,
                    "PDF investigation report downloaded for " + txId);

            String filename = "FraudGuard-Report-" + txId + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .body(pdf);

        } catch (Exception e) {
            log.error("Failed to generate PDF report for {}: {}", txId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
