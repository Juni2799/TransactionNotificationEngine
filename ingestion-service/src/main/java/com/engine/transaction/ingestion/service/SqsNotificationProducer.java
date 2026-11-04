package com.engine.transaction.ingestion.service;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.model.SendAmazonSqsException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SqsNotificationProducer {

    private final SqsTemplate sqsTemplate;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public void sendNotificationEvent(Object payload, String accountId, String transactionId) {
        try {
            log.info("Publishing transaction notification to SQS FIFO for transactionId: {}", transactionId);

            sqsTemplate.send(to -> to
                    .queue(queueUrl)
                    .payload(payload)
                    .header("MessageGroupId", accountId)             // Ensures order per account
                    .header("MessageDeduplicationId", transactionId) // Prevents duplicate delivery
            );

            log.info("Successfully published transactionId: {} to SQS queue", transactionId);
        } catch (SendAmazonSqsException e) {
            log.error("Failed to send message to SQS FIFO for transactionId: {}. Error: {}", transactionId, e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to enqueue transaction notification", e);
        }
    }
}