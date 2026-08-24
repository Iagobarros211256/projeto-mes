# Backlog — Sprint 3: Módulo de Ordens de Serviço (OS)

Escopo original do PRD (Fase 2): "abertura, acompanhamento e encerramento de manutenções."

## Racional de priorização

A tabela `parada` já tem a coluna `ordem_servico_id` desde o Sprint 1 (V2), nullable e
sem FK — reservada exatamente pra esse momento. Uma parte do trabalho aqui é "ativar"
essa ligação: criar a tabela `ordem_servico` e trocar o campo solto por uma FK de verdade.

## Máquina de estados da OS

```
ABERTA ──(atribuir técnico)──> ABERTA (com técnico)
ABERTA (com técnico) ──(iniciar execução)──> EM_ANDAMENTO
EM_ANDAMENTO ──(concluir)──> CONCLUIDA
ABERTA ou EM_ANDAMENTO ──(cancelar)──> CANCELADA
```
`CONCLUIDA` e `CANCELADA` são estados terminais — nenhuma transição sai deles.

## Regras de negócio novas

- **RN05**: não é possível iniciar execução (`EM_ANDAMENTO`) sem um técnico responsável já atribuído.
- **RN06**: só é possível concluir uma OS que esteja `EM_ANDAMENTO`.
- **RN07**: uma OS em estado terminal (`CONCLUIDA`/`CANCELADA`) não aceita mais nenhuma transição de status.

## User Stories

### US11 — Abrir Ordem de Serviço
**Como** Supervisor de Produção, **quero** abrir uma OS a partir de uma parada existente (ou avulsa, direto num equipamento), **para** formalizar a necessidade de manutenção.
- Vincular a uma parada é opcional: preenche `parada.ordem_servico_id` quando informado.
- Estimativa: 5 pts

### US12 — Atribuir técnico responsável
**Como** Gestor de Manutenção, **quero** atribuir um técnico a uma OS aberta, **para** que ele saiba que precisa executá-la.
- Estimativa: 2 pts

### US13 — Iniciar execução
**Como** Técnico de Manutenção, **quero** marcar que comecei a executar uma OS, **para** que o status reflita o trabalho em andamento (RN05).
- Estimativa: 2 pts

### US14 — Concluir Ordem de Serviço
**Como** Técnico de Manutenção, **quero** encerrar uma OS com observações do que foi feito, **para** fechar o ciclo de manutenção (RN06).
- Estimativa: 3 pts

### US15 — Cancelar Ordem de Serviço
**Como** Gestor de Manutenção, **quero** cancelar uma OS que não será mais executada, **para** manter o painel de OS abertas limpo (RN07).
- Estimativa: 2 pts

### US16 — Consultar/listar OS com filtros
**Como** Gestor/Gerente, **quero** consultar OS filtrando por status, equipamento, técnico e período, **para** acompanhar a carga de manutenção.
- Reaproveita o padrão de `Specification` da US08 (não o JPQL com `IS NULL`, já aprendemos essa lição).
- Estimativa: 5 pts

## Itens técnicos

| ID | Item | Estimativa |
|---|---|---|
| T06 | Migration `ordem_servico` + FK real em `parada.ordem_servico_id` (era solta desde Sprint 1) | 3 pts |
| T07 | Testes automatizados (unitários + integração, seguindo o padrão do T05) | 5 pts |

## Fora de escopo deste sprint (backlog futuro)

- Apontamento de peças usadas na OS (depende do módulo de Estoque, Fase 3)
- Notificação em tempo real de OS via WebSocket (reaproveitaria o padrão do T04, mas não é essencial pro MVP do módulo)
- Anexos/fotos na OS

## Permissões

| Ação | Papéis |
|---|---|
| Abrir OS | SUPERVISOR, GESTOR |
| Atribuir técnico | GESTOR |
| Iniciar execução / Concluir | TECNICO, GESTOR |
| Cancelar | GESTOR |
| Consultar (GET) | qualquer autenticado |
