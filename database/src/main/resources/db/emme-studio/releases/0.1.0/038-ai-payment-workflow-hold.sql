-- liquibase formatted sql
-- changeset emme:038-ai-payment-workflow-hold
-- comment: Bind a payment workflow correlation to the held appointment it may confirm.

ALTER TABLE ai_payment_workflow_correlation
    ADD COLUMN appointment_hold_id UUID;

ALTER TABLE ai_payment_workflow_correlation
    ADD CONSTRAINT ai_payment_workflow_correlation_appointment_hold
        FOREIGN KEY (appointment_hold_id) REFERENCES appointment_hold(id);

CREATE INDEX idx_ai_payment_workflow_correlation_hold
    ON ai_payment_workflow_correlation (appointment_hold_id);

-- rollback: DROP INDEX IF EXISTS idx_ai_payment_workflow_correlation_hold;
-- rollback: ALTER TABLE ai_payment_workflow_correlation DROP CONSTRAINT IF EXISTS ai_payment_workflow_correlation_appointment_hold;
-- rollback: ALTER TABLE ai_payment_workflow_correlation DROP COLUMN IF EXISTS appointment_hold_id;
