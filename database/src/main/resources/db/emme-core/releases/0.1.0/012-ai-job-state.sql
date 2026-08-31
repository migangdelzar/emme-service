-- liquibase formatted sql
-- changeset emme:012-ai-job-state splitStatements:false
-- comment: Create the single durable, tenant-isolated AI job state table in the core schema.

CREATE OR REPLACE FUNCTION emme_core.current_tenant_id()
RETURNS UUID
LANGUAGE sql
STABLE
PARALLEL SAFE
RETURNS NULL ON NULL INPUT
AS 'SELECT nullif(current_setting(''app.current_tenant_id'', true), '''')::UUID';

CREATE TABLE IF NOT EXISTS emme_core.ai_job_state (
  job_id UUID PRIMARY KEY, tenant_id UUID NOT NULL, principal_id UUID NOT NULL,
  roles TEXT NOT NULL, conversation_id UUID NOT NULL, workflow_id UUID NOT NULL,
  trace_id TEXT NOT NULL, idempotency_key TEXT NOT NULL, job_type VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL, status VARCHAR(32) NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
  available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_job_status') THEN
    ALTER TABLE emme_core.ai_job_state ADD CONSTRAINT ck_ai_job_status CHECK (status IN ('QUEUED','CLAIMED','COMPLETED','RETRYING','DEAD_LETTER'));
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_ai_job_attempts') THEN
    ALTER TABLE emme_core.ai_job_state ADD CONSTRAINT ck_ai_job_attempts CHECK (attempts >= 0);
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_ai_job_state_ready ON emme_core.ai_job_state (tenant_id, status, available_at);
CREATE INDEX IF NOT EXISTS idx_ai_job_state_claimed ON emme_core.ai_job_state (status, updated_at);
ALTER TABLE emme_core.ai_job_state ENABLE ROW LEVEL SECURITY;
ALTER TABLE emme_core.ai_job_state FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS ai_job_tenant_isolation ON emme_core.ai_job_state;
CREATE POLICY ai_job_tenant_isolation ON emme_core.ai_job_state FOR ALL USING (tenant_id = emme_core.current_tenant_id()) WITH CHECK (tenant_id = emme_core.current_tenant_id());

-- rollback: DROP TABLE IF EXISTS emme_core.ai_job_state;
-- rollback: DROP FUNCTION IF EXISTS emme_core.current_tenant_id();
