package com.frauddetection.controller;

import com.frauddetection.model.AuditLog;
import com.frauddetection.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "30") int size,
                       Model model) {
        Page<AuditLog> logs = auditLogService.findAll(
                PageRequest.of(page, size, Sort.by("timestamp").descending()));

        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageTitle", "Audit Logs");
        model.addAttribute("activePage", "audit");
        return "audit/list";
    }
}
