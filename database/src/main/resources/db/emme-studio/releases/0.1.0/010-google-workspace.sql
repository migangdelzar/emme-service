-- liquibase formatted sql
-- changeset emme:010-google-workspace
-- comment: Google OAuth tokens and spreadsheet links for Workspace integration.

CREATE TABLE IF NOT EXISTS google_oauth_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    persona_type VARCHAR(10) NOT NULL CHECK (persona_type IN ('STAFF', 'CLIENT')),
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    scopes VARCHAR(255) NOT NULL DEFAULT '',
    expires_at TIMESTAMPTZ NOT NULL,
    provider_email VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, user_id, persona_type)
);

ALTER TABLE google_oauth_token ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON google_oauth_token;
CREATE POLICY tenant_isolation ON google_oauth_token
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_google_oauth_token_tenant ON google_oauth_token(tenant_id);

CREATE TABLE IF NOT EXISTS google_spreadsheet_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    spreadsheet_id VARCHAR(255) NOT NULL,
    spreadsheet_url VARCHAR(1024) NOT NULL,
    export_type VARCHAR(20) NOT NULL CHECK (export_type IN ('APPOINTMENTS', 'CLIENTS', 'FULL')),
    last_exported_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE google_spreadsheet_link ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON google_spreadsheet_link;
CREATE POLICY tenant_isolation ON google_spreadsheet_link
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_google_spreadsheet_link_tenant ON google_spreadsheet_link(tenant_id);

-- rollback: DROP TABLE IF EXISTS google_spreadsheet_link, google_oauth_token CASCADE;
