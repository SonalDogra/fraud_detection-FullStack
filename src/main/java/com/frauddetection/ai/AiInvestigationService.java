package com.frauddetection.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.engine.BehaviourIntelligenceEngine;
import com.frauddetection.engine.DeviceTrustEngine;
import com.frauddetection.engine.RiskAggregationEngine;
import com.frauddetection.engine.RuleIntelligenceEngine;
import com.frauddetection.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiInvestigationService {

    @Value("${ai.provider.url}")
    private String apiUrl;

    @Value("${ai.provider.key}")
    private String apiKey;

    @Value("${ai.provider.model}")
    private String model;

    @Value("${ai.demo.mode:true}")
    private boolean demoMode;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateInvestigationReport(
            Transaction transaction,
            RuleIntelligenceEngine.RuleResult ruleResult,
            BehaviourIntelligenceEngine.BehaviourResult behaviourResult,
            DeviceTrustEngine.DeviceResult deviceResult,
            RiskAggregationEngine.AggregatedRisk aggregatedRisk) {

        if (demoMode || apiKey.equals("demo-key")) {
            return generateDemoReport(transaction, ruleResult, behaviourResult, deviceResult, aggregatedRisk);
        }

        String prompt = buildPrompt(transaction, ruleResult, behaviourResult, deviceResult, aggregatedRisk);

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", new Object[]{
                            Map.of("role", "system", "content",
                                    "You are an expert AI fraud analyst. Generate concise, professional investigation reports."),
                            Map.of("role", "user", "content", prompt)
                    },
                    "max_tokens", 800,
                    "temperature", 0.3
            ));

            Request request = new Request.Builder()
                    .url(apiUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    var json = objectMapper.readTree(response.body().string());
                    return json.at("/choices/0/message/content").asText();
                }
            }
        } catch (Exception e) {
            log.warn("AI API call failed, falling back to demo report: {}", e.getMessage());
        }

        return generateDemoReport(transaction, ruleResult, behaviourResult, deviceResult, aggregatedRisk);
    }

    public String chatWithAssistant(String userMessage, String context) {
        if (demoMode || apiKey.equals("demo-key")) {
            return generateDemoChatResponse(userMessage, context);
        }

        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            String systemPrompt = "You are FraudGuard AI, an expert fraud investigation assistant. " +
                    "You help fraud analysts understand transactions, risk scores, and fraud patterns. " +
                    "Context: " + context;

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    },
                    "max_tokens", 500,
                    "temperature", 0.5
            ));

            Request request = new Request.Builder()
                    .url(apiUrl + "/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    var json = objectMapper.readTree(response.body().string());
                    return json.at("/choices/0/message/content").asText();
                }
            }
        } catch (Exception e) {
            log.warn("AI chat call failed: {}", e.getMessage());
        }

        return generateDemoChatResponse(userMessage, context);
    }

    private String buildPrompt(Transaction tx,
                                RuleIntelligenceEngine.RuleResult rule,
                                BehaviourIntelligenceEngine.BehaviourResult behaviour,
                                DeviceTrustEngine.DeviceResult device,
                                RiskAggregationEngine.AggregatedRisk risk) {
        return String.format("""
            Analyse this transaction for fraud and generate an investigation report:

            Transaction: %s | Amount: %s %s | Customer: %s
            Merchant: %s (%s) | Country: %s | Device: %s (%s)
            VPN: %s | New Device: %s | High-Risk Country: %s

            RULE ENGINE: Score=%.1f, Confidence=%.2f
            Triggered Rules: %s
            %s

            BEHAVIOUR ENGINE: Score=%.1f, Confidence=%.2f
            %s

            DEVICE ENGINE: Trust=%.1f, Risk=%.1f, Confidence=%.2f
            %s

            FINAL RISK SCORE: %d/100 | Level: %s | Confidence: %.2f
            Score Breakdown: %s
            Recommended Action: %s

            Generate a professional investigation report with:
            1. Executive Summary (2-3 sentences)
            2. Key Risk Factors
            3. Behaviour Analysis
            4. Device & Location Assessment
            5. Final Recommendation
            """,
                tx.getTransactionId(), tx.getAmount(), tx.getCurrency(), tx.getCustomerName(),
                tx.getMerchantName(), tx.getMerchantCategory(), tx.getCountry(),
                tx.getDeviceType(), tx.getBrowser(),
                tx.isVpn(), tx.isNewDevice(), tx.isHighRiskCountry(),
                rule.ruleScore(), rule.confidence(), rule.triggeredRules(), rule.explanation(),
                behaviour.behaviourScore(), behaviour.confidence(), behaviour.explanation(),
                device.trustScore(), device.riskScore(), device.confidence(), device.explanation(),
                risk.finalScore(), risk.riskLevel(), risk.confidence(), risk.scoreBreakdown(), risk.recommendedAction()
        );
    }

    private String generateDemoReport(Transaction tx,
                                       RuleIntelligenceEngine.RuleResult rule,
                                       BehaviourIntelligenceEngine.BehaviourResult behaviour,
                                       DeviceTrustEngine.DeviceResult device,
                                       RiskAggregationEngine.AggregatedRisk risk) {
        return String.format("""
            ## AI Investigation Report — %s

            **Executive Summary**
            Transaction %s for %s %s by customer %s has been flagged with a risk score of %d/100 (%s).
            %d fraud rules were triggered during analysis, and the behaviour engine detected %s.

            **Key Risk Factors**
            %s
            %s

            **Behaviour Analysis**
            %s

            **Device & Location Assessment**
            %s

            **Score Breakdown**
            %s

            **Final Recommendation**
            %s

            *This report was generated by FraudGuard AI. An OpenAI API key can be configured for enhanced analysis.*
            """,
                risk.riskLevel(),
                tx.getTransactionId(), tx.getAmount(), tx.getCurrency(), tx.getCustomerName(),
                risk.finalScore(), risk.riskLevel(),
                rule.triggeredRules().size(),
                behaviour.behaviourScore() > 60 ? "unusual spending patterns" : "normal spending patterns",
                rule.explanation().isEmpty() ? "No fraud rules triggered." : rule.explanation(),
                device.explanation(),
                behaviour.explanation(),
                device.explanation(),
                risk.scoreBreakdown(),
                risk.recommendedAction()
        );
    }

    private String generateDemoChatResponse(String message, String context) {
        String lower = message.toLowerCase();
        if (lower.contains("risk score") || lower.contains("score")) {
            return "The risk score is calculated by combining outputs from three engines: Rule Intelligence (35%), Behaviour Analysis (30%), and Device Trust (20%). Each engine evaluates different aspects of the transaction and contributes a weighted score to the final result. A score above 80 triggers an immediate block recommendation.";
        } else if (lower.contains("block") || lower.contains("why")) {
            return "Transactions are blocked when the aggregated risk score exceeds 80/100. This threshold combines rule violations (such as large amounts, VPN usage, new devices), behavioural anomalies (unusual spending patterns), and device trust scores. All decisions include detailed explanations in the investigation report.";
        } else if (lower.contains("similar") || lower.contains("fraud case")) {
            return "The Fraud Similarity Engine stores confirmed fraud cases and uses pattern matching to identify similar transactions. It evaluates amount ranges, merchant categories, geographic patterns, and device fingerprints to surface the most relevant historical cases for each new investigation.";
        } else if (lower.contains("today") || lower.contains("summary")) {
            return "Today's fraud activity summary shows transactions processed through the AI pipeline. High-risk and critical transactions are automatically escalated for manual review. Use the Dashboard for real-time analytics including risk distribution charts and recent alert feeds.";
        } else if (lower.contains("report")) {
            return "To generate an investigation report, navigate to the Transaction Investigation page for any flagged transaction and click 'Generate PDF Report'. The report includes the executive summary, risk factors, behaviour analysis, device assessment, similar cases, and the recommended action — ready for case documentation.";
        }
        return "I am FraudGuard AI, your fraud investigation assistant. I can help you understand risk scores, explain why transactions were flagged, identify similar fraud patterns, summarize today's activity, or guide you through the investigation workflow. To enable full AI analysis, configure an OpenAI API key in the environment settings.";
    }
}
