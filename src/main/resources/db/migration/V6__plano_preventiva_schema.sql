CREATE TABLE plano_preventiva (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipamento_id            UUID         NOT NULL REFERENCES equipamento (id),
    titulo                    VARCHAR(150) NOT NULL,
    descricao                 TEXT,
    intervalo_dias            INTEGER,
    intervalo_horas           INTEGER,
    data_ultima_execucao      TIMESTAMP,
    horas_na_ultima_execucao  INTEGER,
    -- Sem FK de propósito: vínculo opcional e explícito com uma OS (US20), mesmo padrão
    -- que parada.ordem_servico_id teve entre o Sprint 1 e o Sprint 3.
    ultima_ordem_servico_id   UUID,
    ativo                     BOOLEAN      NOT NULL DEFAULT TRUE,
    data_criacao              TIMESTAMP    NOT NULL
);

CREATE INDEX idx_plano_preventiva_equipamento ON plano_preventiva (equipamento_id);
CREATE INDEX idx_plano_preventiva_ativo ON plano_preventiva (ativo);
