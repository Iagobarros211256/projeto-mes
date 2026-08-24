# Modelo de Dados — Sprint 3 (Ordens de Serviço)

## Nova entidade: ordem_servico

| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| equipamento_id | UUID (FK -> equipamento) | |
| titulo | VARCHAR | |
| descricao | TEXT | |
| tipo | VARCHAR | CORRETIVA / PREVENTIVA |
| prioridade | VARCHAR | BAIXA / MEDIA / ALTA / URGENTE |
| status | VARCHAR | ABERTA / EM_ANDAMENTO / CONCLUIDA / CANCELADA |
| solicitante_id | UUID (FK -> usuario) | quem abriu a OS |
| tecnico_responsavel_id | UUID (FK -> usuario, nullable) | atribuído depois da abertura (US12) |
| data_abertura | TIMESTAMP | |
| data_inicio_execucao | TIMESTAMP (nullable) | preenchido na US13 |
| data_conclusao | TIMESTAMP (nullable) | preenchido na US14 |
| observacoes_encerramento | TEXT (nullable) | |

## Alteração em tabela existente: parada

`ordem_servico_id` existe desde o Sprint 1 (migration V2) como `UUID` **sem FK** — decisão
consciente na época, documentada no código, exatamente para este momento. Sprint 3 adiciona
a constraint de verdade:

```sql
ALTER TABLE parada
    ADD CONSTRAINT fk_parada_ordem_servico
    FOREIGN KEY (ordem_servico_id) REFERENCES ordem_servico (id);
```

Continua nullable — nem toda parada vira uma OS.

## Relacionamentos novos

- `equipamento` 1 --- N `ordem_servico`
- `usuario` 1 --- N `ordem_servico` (como solicitante)
- `usuario` 1 --- N `ordem_servico` (como técnico responsável)
- `ordem_servico` 1 --- N `parada` (uma OS pode ter sido aberta a partir de, ou estar
  vinculada a, várias paradas ao longo do tempo)
