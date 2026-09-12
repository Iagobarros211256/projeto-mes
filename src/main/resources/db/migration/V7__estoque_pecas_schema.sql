CREATE TABLE peca (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo            VARCHAR(50)  NOT NULL UNIQUE,
    nome              VARCHAR(150) NOT NULL,
    descricao         TEXT,
    unidade_medida    VARCHAR(20)  NOT NULL,
    quantidade_atual  INTEGER      NOT NULL DEFAULT 0,
    estoque_minimo    INTEGER      NOT NULL DEFAULT 0,
    data_criacao      TIMESTAMP    NOT NULL
);

CREATE TABLE movimentacao_estoque (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    peca_id           UUID        NOT NULL REFERENCES peca (id),
    tipo              VARCHAR(20) NOT NULL,
    quantidade        INTEGER     NOT NULL,
    observacao        TEXT,
    responsavel_id    UUID        NOT NULL REFERENCES usuario (id),
    data_hora         TIMESTAMP   NOT NULL,
    -- FK real desde já: diferente de parada.ordem_servico_id (Sprint 1) e
    -- plano_preventiva.ultima_ordem_servico_id (Sprint 4), o módulo de OS já existe
    -- no momento em que esta tabela nasce — não precisa do truque "campo solto".
    ordem_servico_id  UUID REFERENCES ordem_servico (id)
);

CREATE INDEX idx_movimentacao_estoque_peca ON movimentacao_estoque (peca_id);
CREATE INDEX idx_movimentacao_estoque_data ON movimentacao_estoque (data_hora);
