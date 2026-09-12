CREATE TABLE apontamento_producao (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipamento_id        UUID      NOT NULL REFERENCES equipamento (id),
    data_hora_inicio      TIMESTAMP NOT NULL,
    data_hora_fim         TIMESTAMP NOT NULL,
    quantidade_produzida  INTEGER   NOT NULL,
    quantidade_boa        INTEGER   NOT NULL,
    responsavel_id        UUID      NOT NULL REFERENCES usuario (id),
    data_registro         TIMESTAMP NOT NULL
);

CREATE INDEX idx_apontamento_producao_equipamento ON apontamento_producao (equipamento_id);
CREATE INDEX idx_apontamento_producao_periodo ON apontamento_producao (data_hora_inicio, data_hora_fim);

-- Nullable — só é preenchido quando o Gestor configura (US28). Sem isso, Performance
-- do Dashboard vem null, não um valor inventado (ver backlog-sprint6.md).
ALTER TABLE equipamento ADD COLUMN tempo_ciclo_ideal_segundos INTEGER;
