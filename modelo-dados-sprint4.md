# Modelo de Dados — Sprint 4 (Manutenção Preventiva)

## Nova entidade: plano_preventiva

| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| equipamento_id | UUID (FK -> equipamento) | |
| titulo | VARCHAR | ex.: "Troca de óleo", "Lubrificação de rolamentos" |
| descricao | TEXT (nullable) | |
| intervalo_dias | INTEGER (nullable) | periodicidade por calendário |
| intervalo_horas | INTEGER (nullable) | periodicidade por horas de operação |
| data_ultima_execucao | TIMESTAMP (nullable) | null = nunca executado (considerado vencido) |
| horas_na_ultima_execucao | INTEGER (nullable) | horômetro do equipamento no momento da última execução |
| ultima_ordem_servico_id | UUID (nullable) | vínculo opcional e explícito com uma OS (US20) — sem FK, mesmo padrão que `parada.ordem_servico_id` teve no Sprint 1 antes de virar FK real |
| ativo | BOOLEAN | default true (US21) |
| data_criacao | TIMESTAMP | |

Constraint de negócio (RN08 — pelo menos um intervalo setado) é validada na aplicação,
não no banco (não dá pra expressar "OR" entre colunas nullable de forma simples e portável
em CHECK constraint sem duplicar a regra em dois lugares).

## Nenhuma migration nova em equipamento

`horas_operacao_acumuladas` já existe desde V1. Este sprint só passa a **escrever** nesse
campo (via novo endpoint em `EquipamentoController`) — não muda o schema.

## Relacionamentos novos

- `equipamento` 1 --- N `plano_preventiva`
