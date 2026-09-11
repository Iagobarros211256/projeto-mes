# Backlog — Sprint 5: Módulo de Estoque de Peças

Escopo original do PRD: "entrada, saída, estoque mínimo e histórico de utilização."

## Racional de priorização

O backlog da US11 (Sprint 3) já tinha registrado "apontamento de peças usadas na OS" como
fora de escopo, "depende do módulo de Estoque". Esse módulo é o que faltava pra fechar
esse ciclo — igual aconteceu com `ordem_servico_id` (Sprint 1 → 3) e `horas_operacao_acumuladas`
(Sprint 1 → 4).

## Decisão de escopo: sem campo `ativo` em Peça

Diferente de Equipamento e Ordem de Serviço, não vou incluir ativar/inativar peça neste
sprint — reduz uma ação e uma permissão sem tirar nada essencial do RF original. Fica
registrado como possível item futuro, não como esquecimento.

## Regras de negócio novas

- **RN10**: uma saída não pode deixar o estoque da peça negativo (`quantidadeAtual - quantidade >= 0`).
- **RN11**: a quantidade de qualquer movimentação precisa ser positiva (validação simples, `@Positive`).

## Decisão técnica: estoque é um contador mantido, não calculado a cada leitura

`peca.quantidade_atual` é atualizado em cada movimentação (mesmo padrão de
`equipamento.horas_operacao_acumuladas` e `parada.duracao_minutos`) — não é somado a
partir do histórico de movimentações a cada consulta. Mais rápido de ler, e o histórico
completo continua existindo em `movimentacao_estoque` para auditoria/consulta (US26).

**Risco documentado, não resolvido neste sprint:** a validação da RN10 e o decremento do
estoque não são atômicos contra saídas concorrentes na mesma peça (janela de corrida
teórica, mesmo tipo de observação já registrada na RN02 do Sprint 1). Baixo risco dado o
volume de uma fábrica; melhoria futura seria um `SELECT ... FOR UPDATE` ou constraint de
banco.

## User Stories

### US22 — Cadastrar peça
**Como** Gestor de Manutenção, **quero** cadastrar uma peça (código, nome, unidade de medida, estoque mínimo), **para** começar a controlar o estoque dela.
- Estimativa: 2 pts

### US23 — Registrar entrada de peça
**Como** Gestor/Técnico, **quero** registrar a entrada de peças no estoque, **para** manter a quantidade disponível atualizada.
- Estimativa: 2 pts

### US24 — Registrar saída de peça
**Como** Gestor/Técnico, **quero** registrar a saída de peças (opcionalmente vinculada a uma OS), **para** dar baixa no estoque quando uma peça é usada numa manutenção (RN10).
- Vínculo com OS segue o mesmo padrão da Preventiva (Sprint 4): explícito e opcional, sem gatilho automático entre módulos.
- Estimativa: 3 pts

### US25 — Consultar peças com estoque baixo
**Como** Gestor de Manutenção, **quero** ver quais peças estão abaixo do estoque mínimo, **para** saber o que precisa repor.
- Estimativa: 2 pts

### US26 — Consultar histórico de movimentações com filtros
**Como** Gestor de Manutenção, **quero** consultar o histórico de entradas/saídas filtrando por peça, tipo e período, **para** auditar o uso do estoque.
- Estimativa: 3 pts

## Itens técnicos

| ID | Item | Estimativa |
|---|---|---|
| T10 | Migration `peca` + `movimentacao_estoque` (com FK real pra `ordem_servico` desde já — o módulo já existe, sem necessidade do truque do Sprint 1) | 2 pts |
| T11 | Testes automatizados (unitários + integração) | 5 pts |

## Fora de escopo deste sprint

- Ativar/inativar peça (ver decisão acima)
- Alerta automático (e-mail/notificação) de estoque baixo — igual à US19 da Preventiva, só expõe o dado
- Unidade de medida como enum fechado — fica como texto livre por ora (UN, L, KG, M...)

## Permissões

| Ação | Papéis |
|---|---|
| Cadastrar peça | GESTOR |
| Entrada / saída | GESTOR, TECNICO |
| Consultar (GET) | qualquer autenticado |
