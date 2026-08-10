FROM debian:bookworm-slim AS jdbc-driver

USER root
ARG POSTGRESQL_JDBC_SHA256=2a32a9dcbc42d67a50ad3a0de5efd102c8d2be46720045f2cbd6689f160ab7c7
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*
RUN curl -fsSL https://jdbc.postgresql.org/download/postgresql-42.7.8.jar -o /postgresql.jar \
    && echo "${POSTGRESQL_JDBC_SHA256}  /postgresql.jar" | sha256sum -c -

FROM liquibase/liquibase:5.0.2

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*

COPY database/src/main/resources/db /liquibase/changelog/db
COPY database/docker/run-migrations.sh /liquibase/run-migrations.sh
COPY --from=jdbc-driver /postgresql.jar /liquibase/lib/postgresql.jar
RUN chmod 0644 /liquibase/lib/postgresql.jar \
    && sed -i 's/\r$//' /liquibase/run-migrations.sh \
    && chmod 0755 /liquibase/run-migrations.sh

USER liquibase
ENTRYPOINT ["/liquibase/run-migrations.sh"]
