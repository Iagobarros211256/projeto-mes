# Backlog — Sprint 4: Módulo de Manutenção Preventiva

Escopo original do PRD: "calendário, horas de operação e alertas para revisões."

## Racional de priorização

`equipamento.horas_operacao_acumuladas` existe desde o Sprint 1 (V1), criado "pensando
na Preventiva" — e nunca foi usado até agora. Assim como a `ordem_servico_id` da parada,
essa é outra decisão antiga sendo ativada.

## Decisão de arquitetura: integração com OS é explícita, não automática

Cogitei fazer a conclusão de uma OS `PREVENTIVA` disparar automaticamente o registro de
execução do plano correspondente (efeito colateral entre módulos). Decidi **não fazer
isso agora**:
- Exigiria o módulo `ordemservico` saber que `preventiva` existe (inverteria a direção
  de dependência que já estabelecemos: `preventiva` pode depender de `ordemservico`,
  nunca o contrário — mesma regra do ADR-001).
- Um "efeito colateral automático" entre módulos é o tipo de coisa que fica difícil de
  depurar depois ("por que esse plano foi marcado como executado sozinho?").

Em vez disso: `preventiva` depende de `ordemservico` (unidirecional, sem ciclo), e quem
registra a execução de um plano pode **opcionalmente** informar o `ordemServicoId`
relacionado, de forma explícita — sem gatilho automático.

## Regras de negócio novas

- **RN08**: um plano precisa ter pelo menos um critério de periodicidade — `intervaloDias`
  ou `intervaloHoras` (ou os dois).
- **RN09**: o horômetro (`horas_operacao_acumuladas`) não pode retroceder — só aceita
  valores maiores ou iguais ao atual.

## Critério de "vencido" (RF — alertas para revisões)

Um plano está vencido se **qualquer um** dos critérios configurados for atingido:
- **Por data**: nunca executado, ou `hoje >= dataUltimaExecucao + intervaloDias`
- **Por horas**: nunca executado, ou `horasAtuais - horasNaUltimaExecucao >= intervaloHoras`

## User Stories

### US17 — Cadastrar plano de manutenção preventiva
**Como** Gestor de Manutenção, **quero** cadastrar um plano preventivo por equipamento (o quê, e a cada quanto tempo/uso), **para** formalizar a rotina de manutenção.
- Estimativa: 3 pts

### US18 — Registrar horas de operação de um equipamento
**Como** Gestor de Manutenção, **quero** atualizar o horômetro de um equipamento, **para** que os planos baseados em horas de uso tenham dado real (RN09).
- Estimativa: 2 pts

### US19 — Consultar planos com alerta de vencimento
**Como** Gestor de Manutenção, **quero** ver quais planos estão vencidos, **para** priorizar a abertura de OS preventivas.
- Estimativa: 3 pts

### US20 — Registrar execução de uma revisão preventiva
**Como** Gestor/Técnico, **quero** marcar que uma revisão foi feita (zerando o contador), opcionalmente vinculando a uma OS já existente, **para** reiniciar a contagem de periodicidade.
- Estimativa: 3 pts

### US21 — Ativar/inativar um plano preventivo
**Como** Gestor de Manutenção, **quero** desativar um plano sem apagá-lo, **para** pausar a rotina num equipamento fora de uso sem perder o histórico.
- Estimativa: 2 pts

## Itens técnicos

| ID | Item | Estimativa |
|---|---|---|
| T08 | Migration `plano_preventiva` | 2 pts |
| T09 | Testes automatizados (unitários + integração) | 5 pts |

## Fora de escopo deste sprint

- Disparo automático de alertas (e-mail/notificação) — a US19 expõe o dado, não empurra notificação
- Gatilho automático de OS ao vencer um plano
- Vínculo automático entre conclusão de OS e execução de plano (ver decisão de arquitetura acima)

## Permissões

| Ação | Papéis |
|---|---|
| Cadastrar/ativar/inativar plano | GESTOR |
| Registrar horas de operação | GESTOR, TECNICO |
| Registrar execução | GESTOR, TECNICO |
| Consultar (GET) | qualquer autenticado |
