-- liquibase formatted sql
-- changeset emme:014-ai-semantic-search
-- comment: Tenant and principal-scoped semantic references and cache for AI routing.

CREATE TABLE IF NOT EXISTS ai_intent_reference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    intent_key VARCHAR(80) NOT NULL,
    reference_text VARCHAR(2000) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'es-MX',
    reference_tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('spanish', reference_text)) STORED,
    embedding vector(1024),
    embedding_model_version VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT true,
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, intent_key, locale, reference_text)
);

CREATE TABLE IF NOT EXISTS ai_tool_reference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    tool_key VARCHAR(120) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'es-MX',
    risk_level VARCHAR(30) NOT NULL DEFAULT 'READ_ONLY'
        CHECK (risk_level IN ('READ_ONLY', 'CONFIRMATION_REQUIRED', 'STAFF_APPROVAL')),
    reference_tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('spanish', description)) STORED,
    embedding vector(1024),
    embedding_model_version VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT true,
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, tool_key, locale, description)
);

CREATE TABLE IF NOT EXISTS ai_semantic_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    principal_id UUID NOT NULL,
    cache_kind VARCHAR(40) NOT NULL,
    query_text VARCHAR(4000) NOT NULL,
    context_fingerprint VARCHAR(128) NOT NULL,
    embedding vector(1024) NOT NULL,
    embedding_model_version VARCHAR(150) NOT NULL,
    prompt_version VARCHAR(150) NOT NULL,
    response_payload JSONB NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    expires_at TIMESTAMPTZ NOT NULL,
    hit_count BIGINT NOT NULL DEFAULT 0 CHECK (hit_count >= 0),
    last_hit_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE ai_intent_reference ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_tool_reference ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_semantic_cache ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON ai_intent_reference;
CREATE POLICY tenant_isolation ON ai_intent_reference
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON ai_tool_reference;
CREATE POLICY tenant_isolation ON ai_tool_reference
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON ai_semantic_cache;
CREATE POLICY tenant_isolation ON ai_semantic_cache
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_intent_tenant_active
    ON ai_intent_reference(tenant_id, active);
CREATE INDEX IF NOT EXISTS idx_ai_intent_embedding
    ON ai_intent_reference USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_ai_intent_tsv
    ON ai_intent_reference USING gin (reference_tsv);

CREATE INDEX IF NOT EXISTS idx_ai_tool_tenant_active
    ON ai_tool_reference(tenant_id, active);
CREATE INDEX IF NOT EXISTS idx_ai_tool_embedding
    ON ai_tool_reference USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_ai_tool_tsv
    ON ai_tool_reference USING gin (reference_tsv);

CREATE INDEX IF NOT EXISTS idx_ai_cache_embedding
    ON ai_semantic_cache USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_ai_cache_lookup
    ON ai_semantic_cache(tenant_id, principal_id, active, expires_at);

-- rollback: DROP TABLE IF EXISTS ai_semantic_cache, ai_tool_reference, ai_intent_reference CASCADE;
