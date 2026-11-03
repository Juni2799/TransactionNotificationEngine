-- Create custom ENUM for transaction status lifecycle
CREATE TYPE notification_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'SENT',
    'FAILED',
    'ARCHIVED'
);

-- Create transaction notifications table optimized for high throughput & archiving
CREATE TABLE transaction_notifications (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    account_id VARCHAR(100) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payload_type VARCHAR(50) NOT NULL, -- e.g., 'OTP', 'FRAUD_ALERT'
    recipient_contact VARCHAR(255) NOT NULL,
    status notification_status NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

-- Performance indexes for querying active vs terminal states and archival cron jobs
CREATE INDEX idx_notifications_status_created ON transaction_notifications(status, created_at);
CREATE INDEX idx_notifications_account_id ON transaction_notifications(account_id);
CREATE INDEX idx_notifications_transaction_id ON transaction_notifications(transaction_id);