-- V30 — Create import_jobs table for tracking CSV/Excel import operations
-- Sprint 10A: CSV/Excel Import Infrastructure

CREATE TABLE import_jobs (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name       VARCHAR(500)    NOT NULL,
    file_type       VARCHAR(20)     NOT NULL,   -- CSV | XLSX
    import_type     VARCHAR(50)     NOT NULL,   -- PRODUCT | CUSTOMER | ORDER
    status          VARCHAR(30)     NOT NULL,   -- UPLOADED | VALIDATING | IMPORTING | COMPLETED | PARTIAL_SUCCESS | FAILED
    total_rows      INT             NOT NULL DEFAULT 0,
    successful_rows INT             NOT NULL DEFAULT 0,
    failed_rows     INT             NOT NULL DEFAULT 0,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by      UUID            REFERENCES users(id) ON DELETE SET NULL
);

-- Support filtering by status and type
CREATE INDEX idx_import_jobs_status      ON import_jobs(status);
CREATE INDEX idx_import_jobs_import_type ON import_jobs(import_type);
CREATE INDEX idx_import_jobs_created_at  ON import_jobs(created_at DESC);
CREATE INDEX idx_import_jobs_created_by  ON import_jobs(created_by);
