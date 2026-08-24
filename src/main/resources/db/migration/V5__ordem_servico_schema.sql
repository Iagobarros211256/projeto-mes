CREATE TABLE ordem_servico (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipamento_id           UUID         NOT NULL REFERENCES equipamento (id),
    titulo                   VARCHAR(150) NOT NULL,
    descricao                TEXT,
    tipo                     VARCHAR(20)  NOT NULL,
    prioridade               VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ABERTA',
    solicitante_id           UUID         NOT NULL REFERENCES usuario (id),
    tecnico_responsavel_id   UUID REFERENCES usuario (id),
    data_abertura            TIMESTAMP    NOT NULL,
    data_inicio_execucao     TIMESTAMP,
    data_conclusao           TIMESTAMP,
    observacoes_encerramento TEXT
);

CREATE INDEX idx_ordem_servico_status ON ordem_servico (status);
CREATE INDEX idx_ordem_servico_equipamento ON ordem_servico (equipamento_id);

-- A coluna já existia desde V2 (Sprint 1), sem FK de propósito, esperando o módulo de OS
-- existir. Agora existe.
ALTER TABLE parada
    ADD CONSTRAINT fk_parada_ordem_servico
        FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico (id);
