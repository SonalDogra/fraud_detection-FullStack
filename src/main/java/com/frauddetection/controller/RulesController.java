package com.frauddetection.controller;

import com.frauddetection.model.FraudRule;
import com.frauddetection.repository.FraudRuleRepository;
import com.frauddetection.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RulesController {

    private final FraudRuleRepository ruleRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public String list(Model model) {
        List<FraudRule> rules = ruleRepository.findAll();
        long activeCount = rules.stream().filter(FraudRule::isEnabled).count();
        model.addAttribute("rules", rules);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("categories", FraudRule.RuleCategory.values());
        model.addAttribute("pageTitle", "Rule Engine");
        model.addAttribute("activePage", "rules");
        return "rules/list";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails user,
                         RedirectAttributes redirectAttributes) {
        FraudRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found"));
        rule.setEnabled(!rule.isEnabled());
        rule.setUpdatedAt(LocalDateTime.now());
        ruleRepository.save(rule);
        auditLogService.log(user.getUsername(), "TOGGLE_RULE", "FraudRule", String.valueOf(id),
                "Rule '" + rule.getName() + "' " + (rule.isEnabled() ? "enabled" : "disabled"));
        redirectAttributes.addFlashAttribute("success", "Rule '" + rule.getName() + "' " +
                (rule.isEnabled() ? "enabled" : "disabled") + ".");
        return "redirect:/rules";
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @RequestParam Double weight,
                         @RequestParam Double threshold,
                         @RequestParam Integer riskPoints,
                         @AuthenticationPrincipal UserDetails user,
                         RedirectAttributes redirectAttributes) {
        FraudRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found"));
        rule.setWeight(weight);
        rule.setThreshold(threshold);
        rule.setRiskPoints(riskPoints);
        rule.setUpdatedAt(LocalDateTime.now());
        ruleRepository.save(rule);
        auditLogService.log(user.getUsername(), "UPDATE_RULE", "FraudRule", String.valueOf(id),
                "Rule '" + rule.getName() + "' parameters updated");
        redirectAttributes.addFlashAttribute("success", "Rule updated.");
        return "redirect:/rules";
    }
}
