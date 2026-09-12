# Backlog — Sprint 6: Apontamento de Produção + Dashboard de OEE

Escopo original do PRD: "Dashboard de produção: OEE, tempo parado, disponibilidade das
máquinas e principais causas de parada."

## Por que dois módulos num sprint só

`Disponibilidade` já é 100% calculável com o que existe (Paradas). `Performance` e
`Qualidade` — os outros dois terços da fórmula do OEE — dependem de dado de produção que
nenhum módulo anterior coleta. Em vez de fingir que dava pra pular essa etapa, este sprint
cria o **Apontamento de Produção** primeiro (dado real), e o **Dashboard** depois, como
uma camada de leitura/agregação por cima dele — sem tabela própria.

## Simplificação deliberada: ciclo ideal por equipamento, não por produto

OEE de verdade normalmente calcula performance por **produto** (cada produto tem seu
próprio tempo de ciclo ideal). Não existe módulo de cadastro de produto neste sistema, e
criar um só pra isso seria escopo desproporcional ao pedido original. Simplificação:
`tempo_ciclo_ideal_segundos` vira um campo do **equipamento** — assume-se que cada
equipamento produz essencialmente uma coisa (ou a média das coisas que produz), o que é
razoável pra uma fábrica de biscoitos com linhas dedicadas. Registrado aqui como
simplificação consciente, não como limitação escondida.

## Regras de negócio novas

- **RN12**: `quantidadeBoa` não pode ser maior que `quantidadeProduzida`.
- **RN13**: `dataHoraFim` do apontamento tem que ser depois de `dataHoraInicio`.

## Fórmulas do Dashboard (e quando cada uma fica indisponível, não fake)

- **Tempo planejado** = duração bruta do período consultado (`dataFim - dataInicio`).
  Simplificação: não desconta calendário de turnos/feriados, porque não existe módulo de
  calendário de produção no sistema.
- **Tempo parado** = soma de `duracaoMinutos` das paradas `ENCERRADA` do equipamento no
  período (mesma lógica da US10, escopada a um equipamento).
- **Disponibilidade** = `(tempoPlanejado - tempoParado) / tempoPlanejado` — sempre
  calculável, não depende de apontamento de produção.
- **Performance** = `(tempoCicloIdealSegundos × quantidadeProduzida) / tempoOperacaoSegundos`
  — **null** se o equipamento não tiver `tempoCicloIdealSegundos` configurado, ou se não
  houver apontamento no período. Um valor acima de 1.0 é sinal de ciclo ideal mal
  configurado, não é limitado artificialmente a 1.0.
- **Qualidade** = `quantidadeBoa / quantidadeProduzida` — **null** se não houver
  apontamento no período (não existe "produziu zero, então qualidade é 100%").
- **OEE** = `Disponibilidade × Performance × Qualidade` — **null** se qualquer um dos
  dois últimos for null. O endpoint sempre devolve `disponibilidade`, mesmo quando `oee`
  vem nulo — nunca omite o que dá pra calcular só porque o resto não dá.

## User Stories

### US27 — Registrar apontamento de produção
**Como** Operador/Supervisor, **quero** registrar quanto foi produzido (e quanto foi aprovado) num período, **para** alimentar o cálculo de Performance e Qualidade do OEE.
- Estimativa: 3 pts

### US28 — Configurar tempo de ciclo ideal do equipamento
**Como** Gestor de Manutenção, **quero** definir o tempo de ciclo ideal de um equipamento, **para** que o Dashboard consiga calcular Performance.
- Estimativa: 1 pt

### US29 — Dashboard de OEE por equipamento e período
**Como** Gerente de Produção, **quero** ver Disponibilidade, Performance, Qualidade e OEE de um equipamento num período, **para** identificar os equipamentos mais problemáticos.
- Estimativa: 5 pts

### US30 — Ranking de principais causas de parada
**Como** Gerente de Produção, **quero** ver quais motivos de parada mais consomem tempo, **para** priorizar ação corretiva.
- Estimativa: 3 pts

## Itens técnicos

| ID | Item | Estimativa |
|---|---|---|
| T12 | Migration `apontamento_producao` + `ALTER TABLE equipamento ADD tempo_ciclo_ideal_segundos` | 2 pts |
| T13 | Testes automatizados (unitários + integração), com atenção especial aos casos de OEE `null` | 5 pts |

## Fora de escopo deste sprint

- Cadastro de produto / ciclo ideal por produto (ver simplificação acima)
- Calendário de turnos/feriados pro "tempo planejado"
- Gráficos/visualização — este sprint entrega a API, não uma UI

## Permissões

| Ação | Papéis |
|---|---|
| Registrar apontamento de produção | OPERADOR, SUPERVISOR, GESTOR |
| Configurar tempo de ciclo ideal | GESTOR |
| Consultar dashboard/ranking | qualquer autenticado |
