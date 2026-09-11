# Modelo de Dados — Sprint 5 (Estoque de Peças)

## Nova entidade: peca

| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| codigo | VARCHAR (unique) | |
| nome | VARCHAR | |
| descricao | TEXT (nullable) | |
| unidade_medida | VARCHAR | texto livre por ora (UN, L, KG, M...) |
| quantidade_atual | INTEGER | mantido a cada movimentação, não calculado (ver backlog-sprint5.md) |
| estoque_minimo | INTEGER | |
| data_criacao | TIMESTAMP | |

## Nova entidade: movimentacao_estoque

| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| peca_id | UUID (FK -> peca) | |
| tipo | VARCHAR | ENTRADA / SAIDA |
| quantidade | INTEGER | sempre positivo (RN11) — o sinal vem do `tipo`, não do valor |
| observacao | TEXT (nullable) | |
| responsavel_id | UUID (FK -> usuario) | quem registrou a movimentação |
| data_hora | TIMESTAMP | |
| ordem_servico_id | UUID (FK -> ordem_servico, nullable) | vínculo opcional e explícito (US24) — **com FK real desde já**, diferente do que aconteceu com `parada.ordem_servico_id` no Sprint 1: o módulo de OS já existe agora, não precisa do "campo solto até o módulo nascer" |

## Relacionamentos novos

- `peca` 1 --- N `movimentacao_estoque`
- `usuario` 1 --- N `movimentacao_estoque` (como responsável)
- `ordem_servico` 1 --- N `movimentacao_estoque` (opcional)
