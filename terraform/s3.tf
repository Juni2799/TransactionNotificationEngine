resource "aws_s3_bucket" "archival_bucket" {
  bucket = "transaction-archives-bucket"
}

# Optional: Enable bucket versioning for audit compliance
resource "aws_s3_bucket_versioning" "archival_versioning" {
  bucket = aws_s3_bucket.archival_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}