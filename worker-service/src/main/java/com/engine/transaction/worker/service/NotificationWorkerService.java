package com.engine.transaction.worker.service;

import com.engine.transaction.common.model.TransactionNotification;
import com.engine.transaction.common.repository.TransactionNotificationRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationWorkerService {

    private final TransactionNotificationRepository notificationRepository;

    @SqsListener("${aws.sqs.queue-name}")
    @Transactional
    public void processNotification(Map<String, Object> payload, @Header(required = false, value = "SqsReceiptHandle") String receiptHandle) {
        String transactionId = (String) payload.get("transactionId");
        log.info("Worker received SQS message for transactionId: {}", transactionId);

        // 1. Fetch record from database
        TransactionNotification notification = notificationRepository.findByTransactionId(transactionId)
                .orElse(null);

        if (notification == null) {
            log.error("Notification record not found in DB for transactionId: {}. Skipping processing.", transactionId);
            return;
        }

        try {
            // 2. Update status to PROCESSING
            notification.setStatus(TransactionNotification.NotificationStatus.PROCESSING);
            notification.setUpdatedAt(OffsetDateTime.now());
            notificationRepository.save(notification);

            // 3. Simulate external notification provider call (e.g., Twilio, AWS SNS, Firebase)
            executeExternalNotificationDispatch(notification);

            // 4. Update status to SENT on success
            notification.setStatus(TransactionNotification.NotificationStatus.SENT);
            notification.setProcessedAt(OffsetDateTime.now());
            notification.setUpdatedAt(OffsetDateTime.now());
            notificationRepository.save(notification);

            log.info("Successfully processed and dispatched notification for transactionId: {}", transactionId);

        } catch (Exception e) {
            log.error("Failed to process notification for transactionId: {}. Error: {}", transactionId, e.getMessage());

            // Increment retry counter and capture error message
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification.setErrorMessage(e.getMessage());
            notification.setStatus(TransactionNotification.NotificationStatus.FAILED);
            notification.setUpdatedAt(OffsetDateTime.now());
            notificationRepository.save(notification);

            // Rethrow exception so SQS visibility timeout/redrive policy catches it and retries or routes to DLQ
            throw new RuntimeException("Notification processing failed, triggering SQS retry", e);
        }
    }

    private void executeExternalNotificationDispatch(TransactionNotification notification) throws InterruptedException {
        // Simulate network latency for third-party provider call
        Thread.sleep(500);

        // Optional: Simulate random failure for testing DLQ and retry mechanisms if needed
        // if (Math.random() < 0.2) { throw new RuntimeException("Third-party gateway timeout"); }

        log.info("Dispatched [{}] notification to recipient: {}", notification.getPayloadType(), notification.getRecipientContact());
    }
}