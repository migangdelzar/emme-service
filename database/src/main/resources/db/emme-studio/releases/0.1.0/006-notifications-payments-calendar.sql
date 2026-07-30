-- liquibase formatted sql
-- changeset emme:006-notifications-payments-calendar
-- comment: Notifications, payments, and calendar tables for tenant studio schemas.

CREATE TABLE IF NOT EXISTS notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    channel VARCHAR(30) NOT NULL CHECK (channel IN ('WHATSAPP','WEB','PUSH','EMAIL')),
    recipient_reference VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED' CHECK (status IN ('REQUESTED','SENT','DELIVERED','FAILED','CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE notification ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON notification;
CREATE POLICY tenant_isolation ON notification
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_notification_tenant ON notification(tenant_id);

CREATE TABLE IF NOT EXISTS payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    provider_reference VARCHAR(150) NOT NULL,
    amount DECIMAL(10,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'MXN',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','AUTHORIZED','CAPTURED','DECLINED','REFUNDED','CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, provider_reference)
);

ALTER TABLE payment ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON payment;
CREATE POLICY tenant_isolation ON payment
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_payment_tenant ON payment(tenant_id);

CREATE TABLE IF NOT EXISTS calendar_sync_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL CHECK (provider IN ('GOOGLE_CALENDAR')),
    sync_token VARCHAR(255) NOT NULL DEFAULT '',
    last_synced_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','STALE','FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE calendar_sync_state ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON calendar_sync_state;
CREATE POLICY tenant_isolation ON calendar_sync_state
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE TABLE IF NOT EXISTS calendar_event_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    appointment_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL CHECK (provider IN ('GOOGLE_CALENDAR')),
    external_event_id VARCHAR(150) NOT NULL,
    etag VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SYNCED','CONFLICT','DELETED','FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, provider, external_event_id)
);

ALTER TABLE calendar_event_link ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON calendar_event_link;
CREATE POLICY tenant_isolation ON calendar_event_link
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_calendar_event_link_appointment ON calendar_event_link(appointment_id);

-- rollback: DROP TABLE IF EXISTS calendar_event_link, calendar_sync_state, payment, notification CASCADE;
