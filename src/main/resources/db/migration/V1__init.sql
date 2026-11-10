-- V1: Criação inicial das tabelas
CREATE TABLE workflow (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL
);

CREATE TABLE workflow_action (
    id BIGSERIAL PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    sequence_order INTEGER NOT NULL,
    workflow_id BIGINT NOT NULL,
    CONSTRAINT fk_workflow_action_workflow FOREIGN KEY (workflow_id) REFERENCES workflow(id) ON DELETE CASCADE
);

CREATE TABLE workflow_execution_log (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    CONSTRAINT fk_workflow_log_workflow FOREIGN KEY (workflow_id) REFERENCES workflow(id) ON DELETE CASCADE
);
