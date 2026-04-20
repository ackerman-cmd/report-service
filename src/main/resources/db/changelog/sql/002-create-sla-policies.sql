-- liquibase formatted sql

-- changeset report-service:002-create-sla-policies
CREATE TABLE report_service.sla_policies
(
    id               UUID        NOT NULL PRIMARY KEY,
    priority         VARCHAR(32) NOT NULL UNIQUE,
    response_hours   INT         NOT NULL,
    resolution_hours INT         NOT NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Default SLA thresholds per priority
-- LOW:      первый ответ 24ч,  решение 168ч (7 дней)
-- MEDIUM:   первый ответ 8ч,   решение 72ч  (3 дня)
-- HIGH:     первый ответ 2ч,   решение 24ч
-- CRITICAL: первый ответ 1ч,   решение 4ч
INSERT INTO report_service.sla_policies (id, priority, response_hours, resolution_hours)
VALUES (gen_random_uuid(), 'LOW', 24, 168),
       (gen_random_uuid(), 'MEDIUM', 8, 72),
       (gen_random_uuid(), 'HIGH', 2, 24),
       (gen_random_uuid(), 'CRITICAL', 1, 4);
