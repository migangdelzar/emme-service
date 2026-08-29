-- liquibase formatted sql
-- changeset emme:023-ai-tool-idempotency-lease
-- comment: Bound crash recovery for in-progress AI mutation idempotency claims.

ALTER TABLE ai_tool_idempotency
    ADD COLUMN IF NOT EXISTS lease_expires_at TIMESTAMPTZ;

UPDATE ai_tool_idempotency
SET lease_expires_at = COALESCE(lease_expires_at, CURRENT_TIMESTAMP + INTERVAL '15 minutes')
WHERE status = 'IN_PROGRESS';

ALTER TABLE ai_tool_idempotency
    ALTER COLUMN lease_expires_at SET DEFAULT (CURRENT_TIMESTAMP + INTERVAL '15 minutes');

ALTER TABLE ai_tool_idempotency
    ADD CONSTRAINT ai_tool_idempotency_in_progress_lease
    CHECK (status = 'SUCCEEDED' OR lease_expires_at IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_ai_tool_idempotency_expired_claims
    ON ai_tool_idempotency(tenant_id, principal_id, lease_expires_at)
    WHERE status = 'IN_PROGRESS';

-- rollback: ALTER TABLE ai_tool_idempotency DROP CONSTRAINT IF EXISTS ai_tool_idempotency_in_progress_lease;
-- rollback: DROP INDEX IF EXISTS idx_ai_tool_idempotency_expired_claims;
-- rollback: ALTER TABLE ai_tool_idempotency DROP COLUMN IF EXISTS lease_expires_at;
