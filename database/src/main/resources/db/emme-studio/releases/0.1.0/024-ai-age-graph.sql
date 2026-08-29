-- liquibase formatted sql
-- changeset emme:024-ai-age-graph
-- comment: Optional Apache AGE registry for a disposable tenant-scoped graph projection.

-- The normal pgvector image does not ship AGE. This guarded bootstrap keeps AGE
-- optional; the AGE runtime image installs the extension before migrations run.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'age') THEN
        BEGIN
            CREATE EXTENSION IF NOT EXISTS age;
        EXCEPTION
            WHEN insufficient_privilege THEN
                RAISE NOTICE 'AGE is available but the migration role cannot install it';
        END;
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS ai_age_graph_registry (
    tenant_id UUID NOT NULL PRIMARY KEY,
    graph_name VARCHAR(100) NOT NULL UNIQUE,
    age_enabled BOOLEAN NOT NULL DEFAULT false,
    projection_version BIGINT NOT NULL DEFAULT 0 CHECK (projection_version >= 0),
    last_projected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE ai_age_graph_registry ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON ai_age_graph_registry;
CREATE POLICY tenant_isolation ON ai_age_graph_registry
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_age_graph_projection
    ON ai_age_graph_registry(tenant_id, age_enabled, projection_version);

-- rollback: DROP TABLE IF EXISTS ai_age_graph_registry CASCADE;
