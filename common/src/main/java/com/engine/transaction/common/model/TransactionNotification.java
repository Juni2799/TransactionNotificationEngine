package com.engine.transaction.common.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "payload_type", nullable = false, length = 50)
    private String payloadType;

    @Column(name = "recipient_contact", nullable = false)
    private String recipientContact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "notification_status")
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public enum NotificationStatus {
        PENDING, PROCESSING, SENT, FAILED, ARCHIVED
    }
}