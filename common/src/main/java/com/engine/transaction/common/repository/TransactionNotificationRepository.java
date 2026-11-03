package com.engine.transaction.common.repository;

import com.engine.transaction.common.model.TransactionNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionNotificationRepository extends JpaRepository<TransactionNotification, UUID> {

    Optional<TransactionNotification> findByTransactionId(String transactionId);

    // Custom query to fetch terminal state records older than a specific date for our S3 Archival cron job
    @Query("SELECT t FROM TransactionNotification t WHERE t.status IN ('SENT', 'FAILED') AND t.createdAt < :cutoffDate")
    List<TransactionNotification> findTerminatedNotificationsBefore(@Param("cutoffDate") OffsetDateTime cutoffDate);

    // Bulk update status to ARCHIVED once data is safely uploaded to S3
    @Modifying
    @Query("UPDATE TransactionNotification t SET t.status = 'ARCHIVED', t.updatedAt = CURRENT_TIMESTAMP WHERE t.id IN :ids")
    void markAsArchived(@Param("ids") List<UUID> ids);
}
