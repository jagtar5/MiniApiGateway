-- MAG M-E initial schema (PostgreSQL). Flyway runs this once, in version order.

CREATE TABLE routes (
    id                VARCHAR(100)  PRIMARY KEY,
    path_pattern      VARCHAR(255)  NOT NULL,
    upstream_url      VARCHAR(2048) NOT NULL,
    cache_ttl_seconds INTEGER       NOT NULL DEFAULT 0    CHECK (cache_ttl_seconds >= 0),
    timeout_ms        INTEGER       NOT NULL DEFAULT 5000 CHECK (timeout_ms > 0)
);

CREATE TABLE rate_limit_rules (
    route_id       VARCHAR(100) PRIMARY KEY REFERENCES routes (id),
    algorithm      VARCHAR(50)  NOT NULL,
    max_requests   INTEGER      NOT NULL CHECK (max_requests > 0),
    window_seconds INTEGER      NOT NULL CHECK (window_seconds > 0)
);

CREATE TABLE config_versions (
    id          BIGSERIAL    PRIMARY KEY,
    version     BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    route_count INTEGER      NOT NULL,
    rule_count  INTEGER      NOT NULL
);
