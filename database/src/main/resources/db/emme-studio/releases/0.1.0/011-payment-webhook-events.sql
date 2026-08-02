-- liquibase formatted sql
-- changeset emme:011-payment-webhook-events
-- comment: Durable tenant-scoped payment webhook idempotency keys.

CREATE TABLE IF NOT EXISTS payment_webhook_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    provider VARCHAR(40) NOT NULL,
    event_id VARCHAR(200) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, provider, event_id)
);

ALTER TABLE payment_webhook_event ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON payment_webhook_event;
CREATE POLICY tenant_isolation ON payment_webhook_event
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_payment_webhook_event_tenant
    ON payment_webhook_event(tenant_id);

-- rollback: DROP TABLE IF EXISTS payment_webhook_event CASCADE;
