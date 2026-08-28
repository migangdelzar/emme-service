-- liquibase formatted sql
-- changeset emme:016-ai-quote-workflow
-- comment: Durable AI quote workflow, extraction, draft, and human review state.

CREATE TABLE IF NOT EXISTS ai_workflow_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    principal_id UUID NOT NULL,
    conversation_id UUID REFERENCES conversation(id),
    workflow_type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL
        CHECK (status IN (
            'RECEIVED', 'EXTRACTING', 'QUOTE_CALCULATED',
            'NEEDS_STAFF_REVIEW', 'WAITING_FOR_STAFF', 'STAFF_APPROVED',
            'STAFF_EDITED', 'QUOTE_READY', 'SENT_TO_CLIENT', 'FAILED'
        )),
    graph_version VARCHAR(80) NOT NULL,
    input_text VARCHAR(8000),
    image_storage_key VARCHAR(1000),
    idempotency_key VARCHAR(160) NOT NULL,
    state JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS ai_workflow_checkpoint (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL REFERENCES ai_workflow_run(id) ON DELETE CASCADE,
    node_name VARCHAR(120) NOT NULL,
    node_execution_key VARCHAR(160) NOT NULL,
    state JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, workflow_id, node_name, node_execution_key)
);

CREATE TABLE IF NOT EXISTS ai_extraction_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL REFERENCES ai_workflow_run(id) ON DELETE CASCADE,
    model_version VARCHAR(150) NOT NULL,
    prompt_version VARCHAR(150) NOT NULL,
    schema_version VARCHAR(80) NOT NULL,
    attributes JSONB NOT NULL,
    confidence_by_field JSONB NOT NULL DEFAULT '{}',
    ambiguities JSONB NOT NULL DEFAULT '[]',
    validation_status VARCHAR(30) NOT NULL
        CHECK (validation_status IN ('VALID', 'REPAIRED', 'REJECTED')),
    needs_human_review BOOLEAN NOT NULL DEFAULT false,
    raw_output JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, workflow_id)
);

CREATE TABLE IF NOT EXISTS quote_draft (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL REFERENCES ai_workflow_run(id) ON DELETE CASCADE,
    extraction_result_id UUID NOT NULL REFERENCES ai_extraction_result(id),
    template_key VARCHAR(120) NOT NULL,
    template_version VARCHAR(150) NOT NULL,
    required_services JSONB NOT NULL DEFAULT '[]',
    add_ons JSONB NOT NULL DEFAULT '[]',
    min_price DECIMAL(10,2) NOT NULL CHECK (min_price >= 0),
    max_price DECIMAL(10,2) NOT NULL CHECK (max_price >= min_price),
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes >= 1 AND duration_minutes <= 1440),
    currency VARCHAR(3) NOT NULL DEFAULT 'MXN',
    status VARCHAR(30) NOT NULL
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'EDITED', 'READY', 'SENT')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, workflow_id)
);

CREATE TABLE IF NOT EXISTS quote_review_task (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL REFERENCES ai_workflow_run(id) ON DELETE CASCADE,
    quote_draft_id UUID NOT NULL REFERENCES quote_draft(id),
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING_FOR_STAFF'
        CHECK (status IN ('WAITING_FOR_STAFF', 'CLAIMED', 'APPROVED', 'EDITED', 'REJECTED')),
    reviewer_id UUID,
    claimed_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    uncertainty_reasons JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, workflow_id)
);

CREATE TABLE IF NOT EXISTS quote_review_decision (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    review_task_id UUID NOT NULL REFERENCES quote_review_task(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL,
    decision_version BIGINT NOT NULL CHECK (decision_version >= 0),
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVED', 'EDITED', 'REJECTED')),
    edited_attributes JSONB,
    edited_quote JSONB,
    notes VARCHAR(4000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, review_task_id, decision_version)
);

ALTER TABLE ai_workflow_run ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_workflow_checkpoint ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_extraction_result ENABLE ROW LEVEL SECURITY;
ALTER TABLE quote_draft ENABLE ROW LEVEL SECURITY;
ALTER TABLE quote_review_task ENABLE ROW LEVEL SECURITY;
ALTER TABLE quote_review_decision ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON ai_workflow_run;
CREATE POLICY tenant_isolation ON ai_workflow_run
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON ai_workflow_checkpoint;
CREATE POLICY tenant_isolation ON ai_workflow_checkpoint
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON ai_extraction_result;
CREATE POLICY tenant_isolation ON ai_extraction_result
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON quote_draft;
CREATE POLICY tenant_isolation ON quote_draft
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON quote_review_task;
CREATE POLICY tenant_isolation ON quote_review_task
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON quote_review_decision;
CREATE POLICY tenant_isolation ON quote_review_decision
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_workflow_tenant_status
    ON ai_workflow_run(tenant_id, status, updated_at);
CREATE INDEX IF NOT EXISTS idx_ai_workflow_conversation
    ON ai_workflow_run(tenant_id, conversation_id);
CREATE INDEX IF NOT EXISTS idx_ai_checkpoint_workflow
    ON ai_workflow_checkpoint(tenant_id, workflow_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_extraction_workflow
    ON ai_extraction_result(tenant_id, workflow_id);
CREATE INDEX IF NOT EXISTS idx_quote_draft_workflow
    ON quote_draft(tenant_id, workflow_id);
CREATE INDEX IF NOT EXISTS idx_quote_review_waiting
    ON quote_review_task(tenant_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_quote_review_decision_task
    ON quote_review_decision(tenant_id, review_task_id, decision_version);

-- rollback: DROP TABLE IF EXISTS quote_review_decision, quote_review_task, quote_draft, ai_extraction_result, ai_workflow_checkpoint, ai_workflow_run CASCADE;
