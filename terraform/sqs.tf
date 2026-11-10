# Dead Letter Queue for failed transaction notifications
resource "aws_sqs_queue" "transaction_dlq" {
  name                        = "transaction-notifications-dlq.fifo"
  fifo_queue                  = true
  content_based_deduplication = true
  message_retention_seconds   = 1209600 # Retain failed messages for 14 days
}

# Main SQS FIFO Queue with Redrive Policy pointing to DLQ
resource "aws_sqs_queue" "transaction_queue" {
  name                        = "transaction-notifications.fifo"
  fifo_queue                  = true
  content_based_deduplication = true
  visibility_timeout_seconds  = 30

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.transaction_dlq.arn
    maxReceiveCount     = 3 # Route to DLQ after 3 failed processing attempts
  })
}