-- liquibase formatted sql
-- changeset emme:002-initial-studio-schema
-- comment: Initial tenant-owned studio schema.

CREATE TABLE IF NOT EXISTS business_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    time_zone VARCHAR(50) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'es-MX',
    display_name VARCHAR(150),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS operating_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    day_of_week VARCHAR(3) NOT NULL
        CHECK (day_of_week IN ('MON','TUE','WED','THU','FRI','SAT','SUN')),
    opens_at TIME NOT NULL,
    closes_at TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, day_of_week)
);

CREATE TABLE IF NOT EXISTS booking_policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    min_notice_minutes INTEGER NOT NULL DEFAULT 60,
    max_advance_days INTEGER NOT NULL DEFAULT 30,
    cancellation_window_minutes INTEGER NOT NULL DEFAULT 120,
    allow_overlap BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    phone VARCHAR(30),
    email VARCHAR(200),
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'RETIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS channel_participant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('WHATSAPP', 'WEB_CHAT')),
    provider_reference VARCHAR(255) NOT NULL,
    customer_id UUID REFERENCES customer(id),
    consent_status VARCHAR(10) NOT NULL DEFAULT 'UNKNOWN'
        CHECK (consent_status IN ('UNKNOWN', 'GRANTED', 'REVOKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, channel, provider_reference)
);

CREATE TABLE IF NOT EXISTS service (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    category VARCHAR(120) NOT NULL DEFAULT 'GENERAL',
    description VARCHAR(1000),
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes >= 1 AND duration_minutes <= 1440),
    base_price DECIMAL(10,2) NOT NULL CHECK (base_price >= 0),
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'RETIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, code)
);

CREATE TABLE IF NOT EXISTS artist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS artist_capability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    artist_id UUID NOT NULL REFERENCES artist(id),
    service_id UUID NOT NULL REFERENCES service(id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (artist_id, service_id)
);

CREATE TABLE IF NOT EXISTS appointment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customer(id),
    service_id UUID NOT NULL REFERENCES service(id),
    artist_id UUID NOT NULL REFERENCES artist(id),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL CHECK (ends_at > starts_at),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
        CHECK (status IN ('DRAFT','CONFIRMED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW')),
    external_calendar_status VARCHAR(15) NOT NULL DEFAULT 'NOT_SYNCED'
        CHECK (external_calendar_status IN ('NOT_SYNCED','SYNCED','CONFLICT','FAILED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    plan VARCHAR(20) NOT NULL CHECK (plan IN ('STARTER','PRO','ENTERPRISE')),
    status VARCHAR(15) NOT NULL DEFAULT 'TRIAL'
        CHECK (status IN ('TRIAL','ACTIVE','PAST_DUE','SUSPENDED','CANCELLED')),
    period_ends_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE OR REPLACE FUNCTION current_tenant_id()
RETURNS UUID
LANGUAGE sql
STABLE
PARALLEL SAFE
RETURNS NULL ON NULL INPUT
AS 'SELECT nullif(current_setting(''app.current_tenant_id'', true), '''')::UUID';

ALTER TABLE business_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE operating_hours ENABLE ROW LEVEL SECURITY;
ALTER TABLE booking_policy ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
ALTER TABLE channel_participant ENABLE ROW LEVEL SECURITY;
ALTER TABLE service ENABLE ROW LEVEL SECURITY;
ALTER TABLE artist ENABLE ROW LEVEL SECURITY;
ALTER TABLE artist_capability ENABLE ROW LEVEL SECURITY;
ALTER TABLE appointment ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON business_profile;
CREATE POLICY tenant_isolation ON business_profile
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON operating_hours;
CREATE POLICY tenant_isolation ON operating_hours
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON booking_policy;
CREATE POLICY tenant_isolation ON booking_policy
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON customer;
CREATE POLICY tenant_isolation ON customer
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON channel_participant;
CREATE POLICY tenant_isolation ON channel_participant
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON service;
CREATE POLICY tenant_isolation ON service
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON artist;
CREATE POLICY tenant_isolation ON artist
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON artist_capability;
CREATE POLICY tenant_isolation ON artist_capability
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON appointment;
CREATE POLICY tenant_isolation ON appointment
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON subscription;
CREATE POLICY tenant_isolation ON subscription
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_bp_tenant ON business_profile(tenant_id);
CREATE INDEX IF NOT EXISTS idx_oh_tenant ON operating_hours(tenant_id);
CREATE INDEX IF NOT EXISTS idx_bkp_tenant ON booking_policy(tenant_id);
CREATE INDEX IF NOT EXISTS idx_customer_tenant ON customer(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cp_tenant ON channel_participant(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cp_customer ON channel_participant(customer_id);
CREATE INDEX IF NOT EXISTS idx_service_tenant ON service(tenant_id);
CREATE INDEX IF NOT EXISTS idx_artist_tenant ON artist(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ac_tenant ON artist_capability(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ac_artist ON artist_capability(artist_id);
CREATE INDEX IF NOT EXISTS idx_ac_service ON artist_capability(service_id);
CREATE INDEX IF NOT EXISTS idx_appt_tenant ON appointment(tenant_id);
CREATE INDEX IF NOT EXISTS idx_appt_artist_time ON appointment(artist_id, starts_at, ends_at);
CREATE INDEX IF NOT EXISTS idx_appt_starts ON appointment(starts_at);
CREATE INDEX IF NOT EXISTS idx_appt_customer ON appointment(customer_id);
CREATE INDEX IF NOT EXISTS idx_sub_tenant ON subscription(tenant_id);

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

-- rollback: DROP TABLE IF EXISTS shedlock, subscription, appointment, artist_capability, artist, service, channel_participant, customer, booking_policy, operating_hours, business_profile CASCADE;
-- rollback: DROP FUNCTION IF EXISTS current_tenant_id();
