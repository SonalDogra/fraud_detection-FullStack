package com.frauddetection.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class NewTransactionForm {
    private String customerName;
    private String customerId;
    private String customerEmail;

    private String merchantName;
    private String merchantCategory;
    private String merchantId;

    private BigDecimal amount;
    private String currency;

    private String country;
    private String city;
    private String ipAddress;
    private String deviceType;
    private String browser;
    private String os;

    private boolean isVpn;
    private boolean isNewDevice;
    private boolean isHighRiskCountry;
    private boolean isForeignCurrency;
    private boolean isRoundAmount;
}
