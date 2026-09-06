-- liquibase formatted sql
-- changeset emme:035-appointment-holds
-- comment: Store tenant-local appointment holds for durable payment workflows.

CREATE TABLE appointment_hold (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    appointment_id UUID NOT NULL REFERENCES appointment(id),
    expires_at TIMESTAMPTZ NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT appointment_hold_tenant_idempotency_key
        UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_appointment_hold_expiry
    ON appointment_hold (tenant_id, expires_at);

CREATE INDEX idx_appointment_hold_appointment
    ON appointment_hold (tenant_id, appointment_id);

ALTER TABLE appointment_hold ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON appointment_hold
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- rollback: DROP TABLE IF EXISTS appointment_hold;
