-- liquibase formatted sql
-- changeset emme:028-ai-semantic-execution-traces
-- comment: Durable semantic routing, cache, and dependency invalidation outcomes.

CREATE TABLE IF NOT EXISTS ai_semantic_execution (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    principal_id UUID NOT NULL,
    conversation_id UUID REFERENCES conversation(id),
    workflow_id UUID,
    trace_id VARCHAR(128),
    operation VARCHAR(80) NOT NULL,
    outcome VARCHAR(80) NOT NULL,
    top1_similarity DOUBLE PRECISION NOT NULL,
    top2_similarity DOUBLE PRECISION NOT NULL,
    margin DOUBLE PRECISION NOT NULL CHECK (margin >= 0),
    matches JSONB NOT NULL,
    dependency VARCHAR(80),
    dependency_version VARCHAR(160),
    invalidation_context VARCHAR(2000),
    latency_ms BIGINT NOT NULL CHECK (latency_ms >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, id)
);

ALTER TABLE ai_semantic_execution ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ai_semantic_execution;
CREATE POLICY tenant_isolation ON ai_semantic_execution
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_semantic_execution_scope
    ON ai_semantic_execution(tenant_id, operation, created_at);

-- rollback: DROP TABLE IF EXISTS ai_semantic_execution CASCADE;
