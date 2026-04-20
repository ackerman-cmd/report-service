-- liquibase formatted sql

-- changeset report-service:005-add-s3-fields-to-report-history
ALTER TABLE report_service.report_history
    ADD COLUMN s3_key         VARCHAR(512),
    ADD COLUMN s3_url         TEXT,
    ADD COLUMN file_size_bytes BIGINT;
