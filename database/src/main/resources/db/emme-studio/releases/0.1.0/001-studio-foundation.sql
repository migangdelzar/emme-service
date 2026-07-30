-- liquibase formatted sql
-- changeset emme:001-studio-foundation
-- comment: Foundation objects required in each tenant studio schema.

CREATE TABLE IF NOT EXISTS tenant_schema_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schema_name VARCHAR(63) NOT NULL,
    template_name VARCHAR(64) NOT NULL DEFAULT 'emme-studio',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (schema_name, template_name)
);

-- rollback: DROP TABLE IF EXISTS tenant_schema_metadata;
