-- liquibase formatted sql
-- changeset emme:020-ai-learning-evaluations
-- comment: Durable offline evaluation evidence for governed AI learning candidates.

CREATE TABLE IF NOT EXISTS ai_learning_candidate_evaluation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    candidate_id UUID NOT NULL REFERENCES ai_learning_candidate(id) ON DELETE CASCADE,
    evaluation_version VARCHAR(150) NOT NULL,
    dataset_complete BOOLEAN NOT NULL,
    safety_passed BOOLEAN NOT NULL,
    regression_passed BOOLEAN NOT NULL,
    shadow_comparison_passed BOOLEAN NOT NULL,
    canary_passed BOOLEAN NOT NULL,
    metrics JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, candidate_id, evaluation_version)
);

ALTER TABLE ai_learning_candidate_evaluation ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_learning_candidate_evaluation FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON ai_learning_candidate_evaluation;
CREATE POLICY tenant_isolation ON ai_learning_candidate_evaluation
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_learning_candidate_evaluation_scope
    ON ai_learning_candidate_evaluation(tenant_id, candidate_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_learning_candidate_evaluation_version
    ON ai_learning_candidate_evaluation(tenant_id, evaluation_version, created_at);

-- rollback: DROP TABLE IF EXISTS ai_learning_candidate_evaluation CASCADE;
