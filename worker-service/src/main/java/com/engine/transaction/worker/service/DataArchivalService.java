package com.engine.transaction.worker.service;

import com.engine.transaction.common.model.TransactionNotification;
import com.engine.transaction.common.repository.TransactionNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataArchivalService {

    private final TransactionNotificationRepository notificationRepository;
    private final S3Template s3Template;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.archival-bucket}")
    private String archivalBucket;

    @Value("${archival.retention-days:30}")
    private int retentionDays;

    // Executes based on cron expression (default: daily at 2 AM)
    @Scheduled(cron = "${archival.cron-expression:0 0 2 * * ?}")
    @Transactional
    public void archiveTerminatedTransactions() {
        log.info("Starting scheduled data archival job for terminal transactions...");

        OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(retentionDays);
        List<TransactionNotification> oldNotifications = notificationRepository.findTerminatedNotificationsBefore(cutoffDate);

        if (oldNotifications.isEmpty()) {
            log.info("No terminal transactions found older than cutoff date: {}. Skipping archival.", cutoffDate);
            return;
        }

        log.info("Found {} historical transactions eligible for archival.", oldNotifications.size());

        try {
            // 1. Serialize records to JSON and compress using GZIP
            byte[] compressedData = compressToGzip(oldNotifications);

            // 2. Construct S3 Object Key with date partitioning (e.g., archives/2026/06/batch-1718000000.json.gz)
            String timestamp = DateTimeFormatter.ofPattern("yyyy/MM/dd-HHmmss").format(OffsetDateTime.now());
            String s3ObjectKey = String.format("archives/%s/transactions-batch-%s.json.gz", timestamp, UUID.randomUUID());

            log.info("Uploading compressed archive batch to S3 bucket [{}] with key [{}]", archivalBucket, s3ObjectKey);

            // 3. Upload to AWS S3 / LocalStack S3
            s3Template.upload(archivalBucket, s3ObjectKey, new ByteArrayInputStream(compressedData));

            log.info("Successfully uploaded archive to S3. Purging historical records from PostgreSQL database...");

            // 4. Extract IDs and delete from primary database to reclaim storage
            List<UUID> archivedIds = oldNotifications.stream().map(TransactionNotification::getId).toList();
            notificationRepository.deleteAllById(archivedIds);

            log.info("Successfully archived and purged {} transaction logs from PostgreSQL.", archivedIds.size());

        } catch (Exception e) {
            log.error("Failed to execute data archival job. Error: {}", e.getMessage(), e);
        }
    }

    private byte[] compressToGzip(List<TransactionNotification> notifications) throws IOException {
        String jsonPayload = objectMapper.writeValueAsString(notifications);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(jsonPayload.getBytes());
        }

        return byteArrayOutputStream.toByteArray();
    }
}