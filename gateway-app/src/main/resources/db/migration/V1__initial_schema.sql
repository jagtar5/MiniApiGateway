-- V1__initial_schema.sql
-- Flyway migration: initial MAG schema
-- All schema changes go through Flyway; never alter tables manually.
-- SQA: PostgreSQL + Flyway (pinned version) as documented in SCD Section 6.3

-- ============================================================
-- routes — routing rules ordered by priority
-- Corresponds to RouteConfig in gateway-core
-- ============================================================
CREATE TABLE routes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    path_pattern    VARCHAR(512)  NOT NULL,
    upstream_urls   TEXT[]        NOT NULL,
    methods         TEXT[]        NOT NULL DEFAULT '{GET,POST,PUT,DELETE,PATCH}',
    auth_required   BOOLEAN       NOT NULL DEFAULT TRUE,
    cache_enabled   BOOLEAN       NOT NULL DEFAULT FALSE,
    cache_ttl_seconds BIGINT      NOT NULL DEFAULT 60,
    priority        INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX routes_path_priority_uidx ON routes (path_pattern, priority);

-- ============================================================
-- rate_limit_rules — per-route limiter settings
-- ============================================================
CREATE TABLE rate_limit_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id        UUID          NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    algorithm       VARCHAR(64)   NOT NULL,   -- TOKEN_BUCKET | FIXED_WINDOW | SLIDING_WINDOW_COUNTER
    key_strategy    VARCHAR(64)   NOT NULL,   -- JWT_SUBJECT | CLIENT_IP
    rate_limit      INTEGER       NOT NULL,   -- max requests
    window_seconds  INTEGER       NOT NULL,   -- time window
    burst           INTEGER,                  -- optional burst allowance (token bucket only)
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ============================================================
-- config_versions — versioning and last-known-good fallback
-- ============================================================
CREATE TABLE config_versions (
    version         BIGSERIAL     PRIMARY KEY,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      VARCHAR(128)  NOT NULL DEFAULT 'system',
    snapshot_json   JSONB         NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT FALSE
);

-- Only one version is active at a time
CREATE UNIQUE INDEX config_versions_active_uidx ON config_versions (is_active) WHERE is_active = TRUE;
