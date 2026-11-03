#!/bin/bash
set -e

echo "Initializing LocalStack AWS Resources..."

ENDPOINT_URL="http://localhost:4566"
REGION="us-east-1"

# 1. Create SQS Dead Letter Queue (DLQ) FIFO
echo "Creating SQS DLQ FIFO..."
DLQ_URL=$(awslocal sqs create-queue \
  --queue-name transaction-notifications-dlq.fifo \
  --attributes '{ "FifoQueue": "true", "ContentBasedDeduplication": "true" }' \
  --query 'QueueUrl' \
  --output text)

# Get DLQ ARN
DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$DLQ_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

echo "DLQ created: $DLQ_ARN"

# 2. Create Main SQS FIFO Queue with Redrive Policy pointing to DLQ
echo "Creating Main SQS FIFO Queue..."
REDELIVERY_POLICY="{\"deadLetterTargetArn\":\"$DLQ_ARN\",\"maxReceiveCount\":\"3\"}"

awslocal sqs create-queue \
  --queue-name transaction-notifications.fifo \
  --attributes "{
    \"FifoQueue\": \"true\",
    \"ContentBasedDeduplication\": \"true\",
    \"RedrivePolicy\": '$REDELIVERY_POLICY'
  }"

echo "Main FIFO Queue created successfully."

# 3. Create S3 Bucket for Data Archival
echo "Creating S3 Archival Bucket..."
awslocal s3 mb s3://transaction-archives-bucket --region "$REGION"

echo "AWS LocalStack resources provisioned successfully!"