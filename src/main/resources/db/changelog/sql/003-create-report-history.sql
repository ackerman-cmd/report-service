-- liquibase formatted sql

-- changeset report-service:003-create-report-history
CREATE TABLE report_service.report_history
(
    id                 UUID         NOT NULL PRIMARY KEY,
    report_type        VARCHAR(50)  NOT NULL,
    recipient_emails   TEXT         NOT NULL,
    generated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    sent_at            TIMESTAMP,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message      TEXT,
    period_start       TIMESTAMP,
    period_end         TIMESTAMP,
    total_appeals      INT,
    sla_breaches_count INT
);

CREATE INDEX idx_report_history_generated_at ON report_service.report_history (generated_at DESC);
CREATE INDEX idx_report_history_status ON report_service.report_history (status);
