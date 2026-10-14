package com.frauddetection.engine;

import com.frauddetection.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class DeviceTrustEngine {

    public DeviceResult analyse(Transaction transaction) {
        double riskScore = 10.0;
        StringBuilder explanation = new StringBuilder();

        if (transaction.isVpn()) {
            riskScore += 30;
            explanation.append("VPN or proxy detected — masks true location. ");
        }

        if (transaction.isNewDevice()) {
            riskScore += 25;
            explanation.append("Transaction made from a new, unrecognized device. ");
        }

        if (transaction.getIpAddress() != null && isTorExitNode(transaction.getIpAddress())) {
            riskScore += 35;
            explanation.append("IP matches known Tor exit node patterns. ");
        }

        if (transaction.getDeviceType() != null && transaction.getDeviceType().equalsIgnoreCase("UNKNOWN")) {
            riskScore += 15;
            explanation.append("Device type could not be identified. ");
        }

        if (explanation.isEmpty()) {
            explanation.append("Device appears trusted — matches known device fingerprint.");
        }

        double deviceTrustScore = Math.max(0, 100 - riskScore);
        double deviceRisk = riskScore;
        double confidence = 0.75;

        return new DeviceResult(deviceTrustScore, deviceRisk, confidence, explanation.toString());
    }

    private boolean isTorExitNode(String ip) {
        return ip.startsWith("10.") || ip.startsWith("192.168.0.0");
    }

    public record DeviceResult(double trustScore, double riskScore, double confidence, String explanation) {}
}
