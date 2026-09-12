# Modelo de Dados — Sprint 6 (Produção + Dashboard de OEE)

## Nova entidade: apontamento_producao

| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| equipamento_id | UUID (FK -> equipamento) | |
| data_hora_inicio | TIMESTAMP | início do período apontado (turno/janela) |
| data_hora_fim | TIMESTAMP | RN13 — tem que ser depois do início |
| quantidade_produzida | INTEGER | |
| quantidade_boa | INTEGER | RN12 — não pode ser maior que quantidade_produzida |
| responsavel_id | UUID (FK -> usuario) | quem registrou |
| data_registro | TIMESTAMP | quando foi lançado no sistema (pode ser depois do turno em si) |

## Alteração em tabela existente: equipamento

```sql
ALTER TABLE equipamento ADD COLUMN tempo_ciclo_ideal_segundos INTEGER;
```
Nullable — só é preenchido quando o Gestor configura (US28). Sem isso, `Performance` do
Dashboard vem `null`, não um valor inventado.

## Dashboard não tem tabela própria

O Dashboard (US29, US30) é uma camada de **leitura e agregação** sobre `parada`,
`apontamento_producao` e `equipamento` — não persiste nada.

## Relacionamentos novos

- `equipamento` 1 --- N `apontamento_producao`
- `usuario` 1 --- N `apontamento_producao` (como responsável)
