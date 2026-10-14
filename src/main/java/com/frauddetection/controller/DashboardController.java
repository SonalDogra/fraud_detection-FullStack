package com.frauddetection.controller;

import com.frauddetection.service.FraudCaseService;
import com.frauddetection.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;
    private final FraudCaseService caseService;

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        TransactionService.DashboardStats stats = transactionService.getDashboardStats();
        FraudCaseService.CaseStats caseStats = caseService.getStats();

        model.addAttribute("stats", stats);
        model.addAttribute("caseStats", caseStats);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        return "dashboard/index";
    }
}
