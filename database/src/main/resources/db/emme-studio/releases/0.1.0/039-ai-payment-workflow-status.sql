-- liquibase formatted sql
-- changeset emme:039-ai-payment-workflow-status
-- comment: Allow durable appointment/payment workflows to wait for verified payment.

ALTER TABLE ai_workflow_run
    DROP CONSTRAINT IF EXISTS ai_workflow_run_status_check;

ALTER TABLE ai_workflow_run
    ADD CONSTRAINT ai_workflow_run_status_check
    CHECK (status IN (
        'RECEIVED', 'RUNNING', 'WAITING_FOR_CONFIRMATION', 'WAITING_FOR_APPROVAL',
        'WAITING_FOR_PAYMENT', 'CLARIFICATION_REQUIRED', 'SUCCEEDED', 'REJECTED', 'FAILED',
        'EXTRACTING', 'QUOTE_CALCULATED', 'NEEDS_STAFF_REVIEW', 'WAITING_FOR_STAFF',
        'STAFF_APPROVED', 'STAFF_EDITED', 'QUOTE_READY', 'SENT_TO_CLIENT'
    ));

-- rollback: ALTER TABLE ai_workflow_run DROP CONSTRAINT IF EXISTS ai_workflow_run_status_check;
