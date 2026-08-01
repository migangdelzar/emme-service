-- Spring Modulith's JPA event publication entity does not declare a length for
-- serialized events. Test events can exceed H2's default VARCHAR(255), so keep
-- the test schema aligned with the JDBC event-publication schema.
ALTER TABLE event_publication ALTER COLUMN serialized_event VARCHAR(4000);
