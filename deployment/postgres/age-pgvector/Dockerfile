# syntax=docker/dockerfile:1
#
# Apache AGE publishes PostgreSQL images and pgvector publishes PostgreSQL
# images, but neither image includes both extensions. Both images use the same
# PostgreSQL 17/trixie ABI, so copy the official pgvector extension artifacts
# into the official AGE runtime instead of maintaining a compiler toolchain.

FROM pgvector/pgvector:0.8.6-pg17-trixie AS pgvector

FROM apache/age:release_PG17_1.6.0

COPY --from=pgvector /usr/lib/postgresql/17/lib/vector.so \
    /usr/lib/postgresql/17/lib/vector.so
COPY --from=pgvector /usr/share/postgresql/17/extension/vector* \
    /usr/share/postgresql/17/extension/
