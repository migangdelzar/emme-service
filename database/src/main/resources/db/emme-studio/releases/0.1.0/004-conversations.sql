-- liquibase formatted sql
-- changeset emme:004-conversations
-- comment: Conversations module tables for tenant studio schemas.

CREATE TABLE IF NOT EXISTS conversation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL CHECK (channel IN ('WHATSAPP', 'WEB_CHAT')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'CLOSED', 'EXPIRED')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS conversation_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation(id),
    sequence_number INTEGER NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (conversation_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS pending_action (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation(id),
    action_type VARCHAR(20) NOT NULL
        CHECK (action_type IN ('BOOK', 'CANCEL', 'PAY', 'REFUND')),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'REJECTED', 'EXPIRED', 'EXECUTED')),
    details JSONB,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE conversation ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversation_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE pending_action ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON conversation;
CREATE POLICY tenant_isolation ON conversation
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON conversation_event;
CREATE POLICY tenant_isolation ON conversation_event
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON pending_action;
CREATE POLICY tenant_isolation ON pending_action
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_conversation_tenant ON conversation(tenant_id);
CREATE INDEX IF NOT EXISTS idx_conversation_participant ON conversation(participant_id);
CREATE INDEX IF NOT EXISTS idx_conversation_tenant_status ON conversation(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_cevent_conversation ON conversation_event(conversation_id);
CREATE INDEX IF NOT EXISTS idx_cevent_tenant ON conversation_event(tenant_id);
CREATE INDEX IF NOT EXISTS idx_paction_conversation ON pending_action(conversation_id);
CREATE INDEX IF NOT EXISTS idx_paction_tenant ON pending_action(tenant_id);
CREATE INDEX IF NOT EXISTS idx_paction_expires_status ON pending_action(expires_at, status);

-- rollback: DROP TABLE IF EXISTS pending_action, conversation_event, conversation CASCADE;
