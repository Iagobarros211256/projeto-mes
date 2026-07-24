CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE usuario (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome   VARCHAR(150) NOT NULL,
    email  VARCHAR(150) NOT NULL UNIQUE,
    senha  VARCHAR(255) NOT NULL,
    perfil VARCHAR(20)  NOT NULL
);

CREATE TABLE linha_producao (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome      VARCHAR(100) NOT NULL,
    descricao VARCHAR(255)
);

CREATE TABLE equipamento (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    linha_producao_id          UUID NOT NULL REFERENCES linha_producao (id),
    codigo                     VARCHAR(50)  NOT NULL UNIQUE,
    nome                       VARCHAR(150) NOT NULL,
    setor                      VARCHAR(100),
    status                     VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    horas_operacao_acumuladas  INTEGER      NOT NULL DEFAULT 0
);

-- Genérica: usada por qualquer módulo que precise de trilha de auditoria (RNF03).
CREATE TABLE log_auditoria (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entidade    VARCHAR(100) NOT NULL,
    entidade_id UUID         NOT NULL,
    usuario_id  UUID         NOT NULL REFERENCES usuario (id),
    acao        VARCHAR(20)  NOT NULL,
    data_hora   TIMESTAMP    NOT NULL DEFAULT now()
);
