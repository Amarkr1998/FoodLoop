FROM postgis/postgis:16-3.4

# Adds pgvector on top of the official PostGIS image so a single Postgres
# instance can serve geospatial (food/pickup/ngo schemas) and vector/RAG
# (ai schema) workloads in local dev, per docs/architecture/02-database-design.md.
RUN apt-get update \
    # This Postgres build has JIT enabled, so PGXS also links per-function
    # bitcode (.bc) for pgvector; that needs clang-13 (compiler) and the
    # llvm-13 package (llvm-lto, not just the -dev headers dev already pulls
    # in) or `make install` fails with "llvm-lto: not found".
    && apt-get install -y --no-install-recommends build-essential clang-13 llvm-13 git postgresql-server-dev-16 \
    && git clone --branch v0.7.4 --depth 1 https://github.com/pgvector/pgvector.git /tmp/pgvector \
    && cd /tmp/pgvector \
    && make \
    && make install \
    && cd / \
    && rm -rf /tmp/pgvector \
    && apt-get remove -y build-essential clang-13 llvm-13 git postgresql-server-dev-16 \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

COPY init/ /docker-entrypoint-initdb.d/
