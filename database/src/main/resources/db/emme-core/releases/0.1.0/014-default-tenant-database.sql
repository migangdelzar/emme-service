-- Shared-database tenants must carry an explicit routing identity so lifecycle
-- listeners can restore the same database context as request processing.
UPDATE emme_core.tenant
SET database_id = '00000000-0000-0000-0000-000000000000'
WHERE database_id IS NULL;
