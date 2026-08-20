CREATE SCHEMA IF NOT EXISTS ai;

CREATE TABLE ai.agent_run (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    agent_name        TEXT NOT NULL,
    trigger_event_id  UUID,
    status            TEXT NOT NULL DEFAULT 'RUNNING',
    started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at      TIMESTAMPTZ,
    model_provider    TEXT,
    model_name        TEXT,
    prompt_version    TEXT,
    -- A redacted decision summary, never raw chain-of-thought (spec §38).
    outcome_summary   TEXT,
    escalated         BOOLEAN NOT NULL DEFAULT false,
    total_tokens      INTEGER,
    total_cost_usd    NUMERIC(12, 6),

    CONSTRAINT chk_agent_run_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'ESCALATED'))
);

CREATE INDEX idx_agent_run_tenant ON ai.agent_run (tenant_id);
CREATE INDEX idx_agent_run_agent_name ON ai.agent_run (agent_name);

ALTER TABLE ai.agent_run ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai.agent_run FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON ai.agent_run
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));

CREATE TABLE ai.tool_call (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL,
    agent_run_id       UUID NOT NULL REFERENCES ai.agent_run(id),
    tool_name          TEXT NOT NULL,
    input_json         JSONB,
    output_json        JSONB,
    authorized_scope   TEXT,
    status             TEXT NOT NULL,
    latency_ms         INTEGER,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_tool_call_status CHECK (status IN ('SUCCESS', 'DENIED', 'FAILED'))
);

CREATE INDEX idx_tool_call_tenant ON ai.tool_call (tenant_id);
CREATE INDEX idx_tool_call_agent_run ON ai.tool_call (agent_run_id);

ALTER TABLE ai.tool_call ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai.tool_call FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON ai.tool_call
    USING (tenant_id::text = current_setting('app.current_tenant', true))
    WITH CHECK (tenant_id::text = current_setting('app.current_tenant', true));
