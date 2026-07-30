-- liquibase formatted sql
-- changeset emme:008-notification-preference
-- comment: Per-tenant notification preferences.

CREATE TABLE IF NOT EXISTS notification_preference (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL,
    channel         VARCHAR(30)  NOT NULL CHECK (channel IN ('WHATSAPP','EMAIL','PUSH','SMS')),
    enabled         BOOLEAN      NOT NULL DEFAULT true,
    template_policy VARCHAR(40)  NOT NULL DEFAULT 'DEFAULT' CHECK (template_policy IN ('DEFAULT','TENANT_CUSTOM')),
    metadata        JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, channel)
);

ALTER TABLE notification_preference ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON notification_preference;
CREATE POLICY tenant_isolation ON notification_preference FOR ALL USING (tenant_id = current_tenant_id());
CREATE INDEX IF NOT EXISTS idx_notification_pref_tenant ON notification_preference(tenant_id);

-- rollback: DROP TABLE IF EXISTS notification_preference CASCADE;
