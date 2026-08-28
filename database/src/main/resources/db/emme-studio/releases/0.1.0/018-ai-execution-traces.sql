-- liquibase formatted sql
-- changeset emme:018-ai-execution-traces
-- comment: Durable tenant-scoped AI model and tool execution traces.

CREATE TABLE IF NOT EXISTS ai_model_execution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    principal_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation(id),
    workflow_id UUID NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    execution_key VARCHAR(160) NOT NULL,
    operation VARCHAR(80) NOT NULL,
    provider_key VARCHAR(120) NOT NULL,
    model_version VARCHAR(150) NOT NULL,
    prompt_version VARCHAR(150) NOT NULL,
    graph_version VARCHAR(80),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCEEDED', 'FAILED')),
    request_payload JSONB NOT NULL,
    response_payload JSONB,
    latency_ms BIGINT NOT NULL CHECK (latency_ms >= 0),
    input_tokens INTEGER CHECK (input_tokens >= 0),
    output_tokens INTEGER CHECK (output_tokens >= 0),
    total_tokens INTEGER CHECK (total_tokens >= 0),
    estimated_cost DECIMAL(18,8) CHECK (estimated_cost >= 0),
    error_code VARCHAR(160),
    error_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, execution_key)
);

CREATE TABLE IF NOT EXISTS ai_tool_call (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    principal_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation(id),
    workflow_id UUID NOT NULL,
    trace_id VARCHAR(128) NOT NULL,
    call_key VARCHAR(160) NOT NULL,
    tool_key VARCHAR(120) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCEEDED', 'REJECTED', 'FAILED')),
    authorized BOOLEAN NOT NULL,
    user_confirmed BOOLEAN NOT NULL,
    staff_approved BOOLEAN NOT NULL,
    arguments_payload JSONB NOT NULL,
    result_payload JSONB,
    latency_ms BIGINT NOT NULL CHECK (latency_ms >= 0),
    error_code VARCHAR(160),
    error_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, call_key)
);

ALTER TABLE ai_model_execution ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_tool_call ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON ai_model_execution;
CREATE POLICY tenant_isolation ON ai_model_execution
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON ai_tool_call;
CREATE POLICY tenant_isolation ON ai_tool_call
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_model_execution_scope
    ON ai_model_execution(tenant_id, conversation_id, workflow_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_model_execution_provider
    ON ai_model_execution(tenant_id, provider_key, status, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_tool_call_scope
    ON ai_tool_call(tenant_id, conversation_id, workflow_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_tool_call_tool
    ON ai_tool_call(tenant_id, tool_key, status, created_at);

-- rollback: DROP TABLE IF EXISTS ai_tool_call, ai_model_execution CASCADE;
