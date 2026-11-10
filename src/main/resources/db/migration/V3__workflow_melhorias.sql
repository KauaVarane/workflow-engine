-- V3: ativo/inativo, descrição, intervalo do agendamento, logs de passos, usuários e mensagens longas.
-- Idempotente: funciona tanto em banco novo (V1 -> V2 -> V3) quanto em banco criado antes pelo Hibernate.

ALTER TABLE workflow ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE workflow ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE workflow ADD COLUMN IF NOT EXISTS interval_minutes INTEGER;
ALTER TABLE workflow ADD COLUMN IF NOT EXISTS last_triggered_at TIMESTAMP;

ALTER TABLE workflow_action ADD COLUMN IF NOT EXISTS max_retries INTEGER;
UPDATE workflow_action SET max_retries = 0 WHERE max_retries IS NULL;
ALTER TABLE workflow_action ALTER COLUMN max_retries SET DEFAULT 0;
ALTER TABLE workflow_action ALTER COLUMN max_retries SET NOT NULL;

-- Antes: VARCHAR(255). Mensagens de erro maiores quebravam o update do log e a execução ficava "RUNNING" para sempre.
ALTER TABLE workflow_execution_log ALTER COLUMN message TYPE TEXT;

CREATE TABLE IF NOT EXISTS workflow_step_log (
    id BIGSERIAL PRIMARY KEY,
    execution_log_id BIGINT REFERENCES workflow_execution_log(id) ON DELETE CASCADE,
    step_order INTEGER,
    action_type VARCHAR(255),
    status VARCHAR(255),
    message TEXT,
    context_snapshot TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP
);
ALTER TABLE workflow_step_log ALTER COLUMN message TYPE TEXT;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roles VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_workflow_action_workflow ON workflow_action(workflow_id, sequence_order);
CREATE INDEX IF NOT EXISTS idx_execution_log_workflow ON workflow_execution_log(workflow_id);
CREATE INDEX IF NOT EXISTS idx_execution_log_status ON workflow_execution_log(status);
CREATE INDEX IF NOT EXISTS idx_step_log_execution ON workflow_step_log(execution_log_id, step_order);
