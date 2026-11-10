-- Cada workflow passa a ter um dono (usuario que o criou)
ALTER TABLE workflow ADD COLUMN owner_id BIGINT;

ALTER TABLE workflow
    ADD CONSTRAINT fk_workflow_owner FOREIGN KEY (owner_id) REFERENCES users (id);

CREATE INDEX idx_workflow_owner ON workflow (owner_id);

-- Workflows que ja existiam ficam com o admin (ou, se nao houver admin, com o primeiro usuario)
UPDATE workflow
   SET owner_id = COALESCE(
         (SELECT id FROM users WHERE username = 'admin'),
         (SELECT MIN(id) FROM users))
 WHERE owner_id IS NULL;