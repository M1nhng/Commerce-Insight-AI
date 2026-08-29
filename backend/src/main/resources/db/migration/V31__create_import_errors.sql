-- V31 — Create import_errors table for per-row error tracking
-- Sprint 10A: CSV/Excel Import Infrastructure

CREATE TABLE import_errors (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    import_job_id   UUID            NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_number      INT             NOT NULL,
    field_name      VARCHAR(100),
    raw_value       VARCHAR(500),   -- sanitized: max 500 chars, never contains secrets
    error_code      VARCHAR(100)    NOT NULL,
    error_message   VARCHAR(1000)   NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Support efficient job error lookup and pagination by row number
CREATE INDEX idx_import_errors_job_id     ON import_errors(import_job_id);
CREATE INDEX idx_import_errors_row_number ON import_errors(import_job_id, row_number);
CREATE INDEX idx_import_errors_error_code ON import_errors(error_code);
