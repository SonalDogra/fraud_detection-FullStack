package com.frauddetection.controller;

import com.frauddetection.ai.AiInvestigationService;
import com.frauddetection.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final AiInvestigationService aiService;
    private final TransactionService transactionService;

    @GetMapping("/chat")
    public String chatPage(Model model) {
        TransactionService.DashboardStats stats = transactionService.getDashboardStats();
        model.addAttribute("stats", stats);
        model.addAttribute("pageTitle", "AI Analyst Assistant");
        model.addAttribute("activePage", "chat");
        return "chat/index";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "").trim();
        if (message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        TransactionService.DashboardStats stats = transactionService.getDashboardStats();
        String context = String.format(
            "Platform stats: Total transactions=%d, High risk=%d, Confirmed fraud=%d, Pending=%d",
            stats.total(), stats.highRisk(), stats.confirmedFraud(), stats.pending()
        );

        String response = aiService.chatWithAssistant(message, context);
        return ResponseEntity.ok(Map.of("response", response));
    }
}
