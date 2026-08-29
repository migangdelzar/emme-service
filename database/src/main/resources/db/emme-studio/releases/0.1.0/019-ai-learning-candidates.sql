-- liquibase formatted sql
-- changeset emme:019-ai-learning-candidates
-- comment: Durable, reviewable candidates for offline AI enrichment.

CREATE TABLE IF NOT EXISTS ai_learning_candidate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    principal_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation(id),
    workflow_id UUID NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    candidate_key VARCHAR(160) NOT NULL,
    candidate_kind VARCHAR(40) NOT NULL
        CHECK (candidate_kind IN ('INTENT_EXAMPLE', 'TOOL_EXAMPLE')),
    reference_text VARCHAR(4000) NOT NULL,
    reference_fingerprint VARCHAR(64) NOT NULL,
    locale VARCHAR(32) NOT NULL,
    embedding_model_version VARCHAR(150) NOT NULL,
    evidence JSONB NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING_EVALUATION'
        CHECK (status IN (
            'PENDING_EVALUATION', 'EVALUATING', 'REJECTED', 'APPROVED',
            'PROMOTED', 'ROLLED_BACK'
        )),
    decision_reason VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (
        tenant_id,
        principal_id,
        candidate_key,
        reference_fingerprint,
        embedding_model_version
    )
);

ALTER TABLE ai_learning_candidate ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON ai_learning_candidate;
CREATE POLICY tenant_isolation ON ai_learning_candidate
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_learning_candidate_scope
    ON ai_learning_candidate(tenant_id, principal_id, conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_learning_candidate_status
    ON ai_learning_candidate(tenant_id, status, updated_at);

-- rollback: DROP TABLE IF EXISTS ai_learning_candidate CASCADE;
