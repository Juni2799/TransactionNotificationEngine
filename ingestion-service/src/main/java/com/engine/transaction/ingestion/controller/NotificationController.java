package com.engine.transaction.ingestion.controller;

import com.engine.transaction.common.model.TransactionNotification;
import com.engine.transaction.common.repository.TransactionNotificationRepository;
import com.engine.transaction.ingestion.dto.TransactionNotificationRequest;
import com.engine.transaction.ingestion.service.SqsNotificationProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final TransactionNotificationRepository notificationRepository;
    private final SqsNotificationProducer sqsProducer;

    @PostMapping
    public ResponseEntity<?> ingestNotification(@Valid @RequestBody TransactionNotificationRequest request) {
        log.info("Received ingestion request for transactionId: {}", request.getTransactionId());

        // 1. Check for duplicate transaction id in DB (Idempotency check)
        if (notificationRepository.findByTransactionId(request.getTransactionId()).isPresent()) {
            log.warn("Duplicate transaction submission detected for transactionId: {}", request.getTransactionId());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "Duplicate Transaction ID",
                            "transactionId", request.getTransactionId()
                    ));
        }

        // 2. Persist state as PENDING in PostgreSQL
        TransactionNotification notification = TransactionNotification.builder()
                .transactionId(request.getTransactionId())
                .accountId(request.getAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .payloadType(request.getPayloadType())
                .recipientContact(request.getRecipientContact())
                .status(TransactionNotification.NotificationStatus.PENDING)
                .retryCount(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        notificationRepository.save(notification);
        log.info("Saved notification state as PENDING in database for transactionId: {}", request.getTransactionId());

        // 3. Push event asynchronously to SQS FIFO
        sqsProducer.sendNotificationEvent(request, request.getAccountId(), request.getTransactionId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "ACCEPTED",
                "transactionId", request.getTransactionId(),
                "message", "Transaction notification accepted and queued for processing"
        ));
    }
}