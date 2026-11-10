#!/bin/bash
# Buckets da seção 3.6.
source "$(dirname "$0")/00-common.sh"
CORS='{"CORSRules":[{"AllowedOrigins":["http://localhost:5173","http://localhost:4200"],"AllowedMethods":["GET","HEAD"],"AllowedHeaders":["*"],"ExposeHeaders":["Content-Disposition"]}]}'
for b in "$BUCKET_XML" "$BUCKET_DOCUMENTS" "$BUCKET_REPORTS" "$BUCKET_BACKUPS"; do
  awslocal s3api head-bucket --bucket "$b" 2>/dev/null || \
    awslocal s3api create-bucket --bucket "$b" --create-bucket-configuration LocationConstraint="$AWS_DEFAULT_REGION" >/dev/null
  awslocal s3api put-bucket-cors --bucket "$b" --cors-configuration "$CORS"
  log "bucket $b ok"
done
for b in "$BUCKET_DOCUMENTS" "$BUCKET_BACKUPS"; do
  awslocal s3api put-bucket-versioning --bucket "$b" --versioning-configuration Status=Enabled
done
