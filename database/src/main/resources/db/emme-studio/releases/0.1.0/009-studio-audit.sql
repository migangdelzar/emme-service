-- liquibase formatted sql
-- changeset emme:009-studio-audit
-- comment: Per-tenant studio audit events.

CREATE TABLE IF NOT EXISTS studio_audit_event (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    actor_reference  VARCHAR(150) NOT NULL,
    action           VARCHAR(120) NOT NULL,
    outcome          VARCHAR(20)  NOT NULL CHECK (outcome IN ('SUCCEEDED','DENIED','FAILED')),
    occurred_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

ALTER TABLE studio_audit_event ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON studio_audit_event FOR ALL USING (tenant_id = current_tenant_id());
CREATE INDEX IF NOT EXISTS idx_studio_audit_tenant ON studio_audit_event(tenant_id);
CREATE INDEX IF NOT EXISTS idx_studio_audit_actor ON studio_audit_event(actor_reference);
CREATE INDEX IF NOT EXISTS idx_studio_audit_occurred ON studio_audit_event(occurred_at);

-- rollback: DROP TABLE IF EXISTS studio_audit_event CASCADE;
