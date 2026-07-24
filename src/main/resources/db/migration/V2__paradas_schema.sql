CREATE TABLE motivo_parada (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    categoria    VARCHAR(50)  NOT NULL,
    subcategoria VARCHAR(100) NOT NULL,
    descricao    VARCHAR(255)
);

CREATE TABLE parada (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipamento_id    UUID        NOT NULL REFERENCES equipamento (id),
    motivo_id         UUID        NOT NULL REFERENCES motivo_parada (id),
    responsavel_id    UUID        NOT NULL REFERENCES usuario (id),
    -- Nullable de propósito: módulo de Ordens de Serviço ainda não existe (Fase 2).
    -- Sem FK constraint por ora, só a coluna reservada (ver ADR em modelo-dados.md).
    ordem_servico_id  UUID,
    data_hora_inicio  TIMESTAMP   NOT NULL,
    data_hora_fim     TIMESTAMP,
    duracao_minutos   BIGINT,
    observacoes       VARCHAR(500),
    status            VARCHAR(20) NOT NULL DEFAULT 'ABERTA'
);

-- Sustenta a checagem de RN02 (existsByEquipamentoIdAndStatus) sem full scan.
CREATE INDEX idx_parada_equipamento_status ON parada (equipamento_id, status);
