-- Stable tenant roles are shared control-plane definitions. Seed them once so
-- concurrent tenant provisioning does not race to create the same role.
INSERT INTO emme_core.role (code, name, scope, active)
VALUES ('tenant_owner', 'tenant owner', 'TENANT', true),
       ('tenant_staff', 'tenant staff', 'TENANT', true)
ON CONFLICT (code) DO NOTHING;
