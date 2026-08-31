CREATE TABLE IF NOT EXISTS emme_core.ai_job_state (
  job_id UUID PRIMARY KEY, tenant_id UUID NOT NULL, job_type VARCHAR(64) NOT NULL,
  payload TEXT NOT NULL, status VARCHAR(32) NOT NULL, attempts INTEGER NOT NULL DEFAULT 0,
  available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, last_error TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_job_state_ready ON emme_core.ai_job_state (tenant_id, status, available_at);
