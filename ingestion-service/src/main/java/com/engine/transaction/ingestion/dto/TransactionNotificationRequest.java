package com.engine.transaction.ingestion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionNotificationRequest {

    @NotBlank(message = "Transaction ID is required")
    @Size(max = 100, message = "Transaction ID cannot exceed 100 characters")
    private String transactionId;

    @NotBlank(message = "Account ID is required")
    @Size(max = 100, message = "Account ID cannot exceed 100 characters")
    private String accountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g., USD, INR)")
    private String currency;

    @NotBlank(message = "Payload type is required")
    private String payloadType; // e.g., 'OTP', 'FRAUD_ALERT'

    @NotBlank(message = "Recipient contact is required")
    private String recipientContact;
}