-- liquibase formatted sql
-- changeset emme:026-conversation-workflow-resume
-- comment: Separate generic conversation workflow checkpoints, statuses, and review decisions.

ALTER TABLE ai_workflow_run
    DROP CONSTRAINT IF EXISTS ai_workflow_run_status_check;

ALTER TABLE ai_workflow_run
    ADD CONSTRAINT ai_workflow_run_status_check
    CHECK (status IN (
        'RECEIVED', 'RUNNING', 'WAITING_FOR_CONFIRMATION', 'WAITING_FOR_APPROVAL',
        'CLARIFICATION_REQUIRED', 'SUCCEEDED', 'REJECTED', 'FAILED',
        'EXTRACTING', 'QUOTE_CALCULATED', 'NEEDS_STAFF_REVIEW', 'WAITING_FOR_STAFF',
        'STAFF_APPROVED', 'STAFF_EDITED', 'QUOTE_READY', 'SENT_TO_CLIENT'
    ));

ALTER TABLE ai_workflow_checkpoint
    ADD COLUMN IF NOT EXISTS workflow_namespace VARCHAR(80) NOT NULL DEFAULT 'default';

ALTER TABLE ai_workflow_checkpoint
    DROP CONSTRAINT IF EXISTS ai_workflow_checkpoint_tenant_id_workflow_id_node_name_node_execution_key_key;

ALTER TABLE ai_workflow_checkpoint
    ADD CONSTRAINT ai_workflow_checkpoint_namespace_key
    UNIQUE (tenant_id, workflow_id, workflow_namespace, node_name, node_execution_key);

CREATE TABLE IF NOT EXISTS ai_conversation_workflow_review_decision (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL REFERENCES ai_workflow_run(id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    reviewer_id UUID NOT NULL,
    decision VARCHAR(40) NOT NULL,
    clarification JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE ai_conversation_workflow_review_decision ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON ai_conversation_workflow_review_decision;
CREATE POLICY tenant_isolation ON ai_conversation_workflow_review_decision
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_conversation_workflow_review_decision
    ON ai_conversation_workflow_review_decision(tenant_id, workflow_id, created_at);

-- rollback: DROP TABLE IF EXISTS ai_conversation_workflow_review_decision;
