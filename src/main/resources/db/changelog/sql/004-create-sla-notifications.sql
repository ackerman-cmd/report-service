-- liquibase formatted sql

-- changeset report-service:004-create-sla-notifications
CREATE TABLE report_service.sla_notifications
(
    id                UUID        NOT NULL PRIMARY KEY,
    appeal_id         UUID        NOT NULL,
    notification_type VARCHAR(30) NOT NULL, -- SLA_WARNING | SLA_BREACH
    recipient_email   VARCHAR(320) NOT NULL,
    sent_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    appeal_subject    VARCHAR(512),
    appeal_priority   VARCHAR(32),
    sla_deadline      TIMESTAMP,
    age_hours         INT
);

-- Одно уведомление каждого типа на обращение — дубли исключены
CREATE UNIQUE INDEX idx_sla_notif_appeal_type
    ON report_service.sla_notifications (appeal_id, notification_type);

CREATE INDEX idx_sla_notif_sent_at ON report_service.sla_notifications (sent_at DESC);
