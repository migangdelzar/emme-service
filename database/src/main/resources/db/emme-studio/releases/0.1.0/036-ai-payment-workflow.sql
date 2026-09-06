-- liquibase formatted sql
-- changeset emme:036-ai-payment-workflow
-- comment: Store tenant-local checkout links for durable payment workflows.

CREATE TABLE payment_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    provider VARCHAR(80) NOT NULL,
    checkout_url TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT payment_link_tenant_idempotency_key
        UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_payment_link_expiry
    ON payment_link (tenant_id, expires_at);

CREATE INDEX idx_payment_link_workflow
    ON payment_link (tenant_id, workflow_id);

ALTER TABLE payment_link ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON payment_link
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- rollback: DROP TABLE IF EXISTS payment_link;
