-- Create schemas for all modules
CREATE SCHEMA IF NOT EXISTS emme_core;
CREATE SCHEMA IF NOT EXISTS emme_salon;
CREATE SCHEMA IF NOT EXISTS emme_identity;
CREATE SCHEMA IF NOT EXISTS emme_subscriptions;
CREATE SCHEMA IF NOT EXISTS emme_payments;
CREATE SCHEMA IF NOT EXISTS emme_notifications;
CREATE SCHEMA IF NOT EXISTS emme_documents;
CREATE SCHEMA IF NOT EXISTS emme_conversations;

-- emme_core: tenant_registry (not managed by JPA entity, used by TenantContextAspect)
CREATE TABLE IF NOT EXISTS emme_core.tenant_registry (
    tenant_id uuid DEFAULT gen_random_uuid() PRIMARY KEY,
    slug varchar(63) NOT NULL UNIQUE,
    schema_name varchar(63) NOT NULL UNIQUE,
    database_mode varchar(16) NOT NULL DEFAULT 'SHARED',
    database_key varchar(128) NOT NULL DEFAULT 'emme',
    status varchar(24) NOT NULL DEFAULT 'PROVISIONING',
    schema_version varchar(64),
    last_migrated_at timestamptz,
    migration_error text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
-- This SQL script only creates tables that are NOT managed by JPA entities.
-- JPA entity tables are created by spring.jpa.hibernate.ddl-auto=create with
-- spring.jpa.properties.hibernate.hbm2ddl.create_schemas=true (Hibernate 7.x).
