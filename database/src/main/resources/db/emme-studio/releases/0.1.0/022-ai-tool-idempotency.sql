-- liquibase formatted sql
-- changeset emme:022-ai-tool-idempotency
-- comment: Durable tenant-scoped idempotency for authorized AI mutation tools.

CREATE TABLE IF NOT EXISTS ai_tool_idempotency (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    principal_id UUID NOT NULL,
    operation_key VARCHAR(320) NOT NULL,
    tool_key VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'SUCCEEDED')),
    result_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, principal_id, operation_key),
    CHECK (status = 'IN_PROGRESS' OR result_payload IS NOT NULL)
);

ALTER TABLE ai_tool_idempotency ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON ai_tool_idempotency;
CREATE POLICY tenant_isolation ON ai_tool_idempotency
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_tool_idempotency_scope
    ON ai_tool_idempotency(tenant_id, principal_id, operation_key, status);

-- rollback: DROP TABLE IF EXISTS ai_tool_idempotency CASCADE;
