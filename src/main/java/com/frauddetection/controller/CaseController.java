package com.frauddetection.controller;

import com.frauddetection.model.FraudCase;
import com.frauddetection.service.AuditLogService;
import com.frauddetection.service.FraudCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CaseController {

    private final FraudCaseService caseService;
    private final AuditLogService auditLogService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            Model model) {

        Page<FraudCase> cases = caseService.findAll(status, priority, search,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        FraudCaseService.CaseStats stats = caseService.getStats();

        model.addAttribute("cases", cases);
        model.addAttribute("stats", stats);
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("search", search);
        model.addAttribute("statuses", FraudCase.CaseStatus.values());
        model.addAttribute("priorities", FraudCase.CasePriority.values());
        model.addAttribute("pageTitle", "Case Management");
        model.addAttribute("activePage", "cases");
        return "cases/list";
    }

    @GetMapping("/{caseId}")
    public String detail(@PathVariable String caseId, Model model) {
        FraudCase fraudCase = caseService.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        model.addAttribute("fraudCase", fraudCase);
        model.addAttribute("pageTitle", "Case — " + caseId);
        model.addAttribute("activePage", "cases");
        return "cases/detail";
    }

    @PostMapping("/{caseId}/update")
    public String updateCase(
            @PathVariable String caseId,
            @RequestParam String outcome,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes) {

        try {
            FraudCase updated = caseService.updateCase(caseId, outcome, notes, user.getUsername());
            auditLogService.log(user.getUsername(), "UPDATE_CASE", "FraudCase", caseId,
                    "Case updated to outcome: " + outcome);
            redirectAttributes.addFlashAttribute("success", "Case updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update case: " + e.getMessage());
        }

        return "redirect:/cases/" + caseId;
    }
}
