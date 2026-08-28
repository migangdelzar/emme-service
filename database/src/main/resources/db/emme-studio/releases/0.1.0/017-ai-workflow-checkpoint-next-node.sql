-- liquibase formatted sql
-- changeset emme:017-ai-workflow-checkpoint-next-node
-- comment: Preserve the next LangGraph node required to resume a durable workflow.

ALTER TABLE ai_workflow_checkpoint
    ADD COLUMN IF NOT EXISTS next_node_name VARCHAR(120);

-- rollback: ALTER TABLE ai_workflow_checkpoint DROP COLUMN IF EXISTS next_node_name;
