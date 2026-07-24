# Backlog — Módulo de Registro de Paradas de Máquina

## Épicos

| Épico | Objetivo |
|---|---|
| **E1 — Cadastro Base** | Estrutura de linhas de produção e equipamentos |
| **E2 — Taxonomia de Motivos** | Padronização das causas de parada |
| **E3 — Registro de Paradas** | Núcleo do módulo: abrir, encerrar, consultar, corrigir |
| **E4 — Fundação Técnica** | Infraestrutura que sustenta os épicos de negócio (não aparece pro usuário final, mas é pré-requisito) |

---

## E1 — Cadastro Base

### US01 — Cadastrar linha de produção
**Como** Gestor de Manutenção (PCM), **eu quero** cadastrar linhas de produção, **para** organizar os equipamentos por área da fábrica.

*Critérios de aceite:*
```gherkin
Dado que estou autenticado como Gestor de Manutenção
Quando informo nome e descrição de uma nova linha de produção
Então o sistema deve salvar a linha e exibi-la na listagem
```
**Estimativa:** 2 pts

### US02 — Cadastrar equipamento
**Como** Gestor de Manutenção, **eu quero** cadastrar um equipamento vinculado a uma linha, **para** que as paradas possam ser registradas por máquina específica (RF01).

*Critérios de aceite:*
```gherkin
Dado que existe ao menos uma linha de produção cadastrada
Quando cadastro um equipamento informando código, nome, linha e setor
Então o equipamento passa a aparecer na lista de equipamentos daquela linha

Dado que tento cadastrar um equipamento sem vincular a uma linha
Então o sistema deve rejeitar o cadastro com mensagem de campo obrigatório
```
**Estimativa:** 3 pts

### US03 — Editar/inativar equipamento
**Como** Gestor de Manutenção, **eu quero** editar ou inativar um equipamento, **para** manter o cadastro atualizado quando uma máquina é substituída ou desativada.

**Estimativa:** 2 pts — **Sprint 2**

---

## E2 — Taxonomia de Motivos

### US04 — Cadastrar categorias e subcategorias de motivo de parada
**Como** Gestor de Manutenção, **eu quero** cadastrar categorias e subcategorias de motivo de parada, **para** padronizar os registros feitos pelos operadores (RF04).

*Critérios de aceite:*
```gherkin
Dado que estou cadastrando um novo motivo de parada
Quando informo categoria (ex: Mecânica) e subcategoria (ex: Rolamento quebrado)
Então o motivo passa a estar disponível na abertura de uma parada

Dado o conjunto de categorias sugerido no PRD
Então o sistema deve vir pré-carregado com: Mecânica, Elétrica, Falta de Material, Setup/Troca de Formato, Limpeza, Qualidade
```
**Estimativa:** 3 pts (inclui seed/migration Flyway com dados iniciais)

---

## E3 — Registro de Paradas (núcleo do MVP)

### US05 — Abrir registro de parada
**Como** Operador de Linha, **eu quero** abrir um registro de parada informando equipamento, motivo e responsável, **para** documentar a interrupção assim que ela ocorre (RF02).

*Critérios de aceite:*
```gherkin
Dado que estou autenticado como Operador
Quando seleciono um equipamento sem parada em aberto e informo motivo e responsável
Então o sistema cria a parada com status ABERTA e data_hora_inicio = agora

Dado que o equipamento selecionado já possui uma parada em aberto
Quando tento abrir uma nova parada para o mesmo equipamento
Então o sistema deve rejeitar com mensagem "equipamento já possui parada em aberto" (RN02)

Dado que não seleciono nenhuma categoria de motivo
Então o sistema deve rejeitar o registro (RN03 — parada sem motivo não é permitida)
```
**Estimativa:** 5 pts

### US06 — Encerrar parada
**Como** Operador de Linha ou Técnico, **eu quero** encerrar uma parada em aberto, **para** que o sistema calcule automaticamente sua duração (RF03).

*Critérios de aceite:*
```gherkin
Dado uma parada em aberto
Quando informo a data/hora de encerramento
Então o sistema calcula duracao_minutos = fim - início e muda o status para ENCERRADA

Dado que informo uma data/hora de fim anterior ao início
Então o sistema deve rejeitar com mensagem de validação (RN01)
```
**Estimativa:** 3 pts

### US08 — Consultar histórico de paradas com filtros
**Como** Supervisor de Produção, **eu quero** consultar o histórico de paradas filtrando por equipamento, período, motivo e responsável, **para** acompanhar o desempenho do turno (RF05).

**Estimativa:** 5 pts — **Sprint 2**

### US09 — Editar/corrigir apontamento
**Como** Gestor de Manutenção, **eu quero** corrigir um apontamento de parada já registrado, **para** ajustar erros de digitação sem perder rastreabilidade (RF06, RNF03 — toda edição gera log de auditoria).

**Estimativa:** 3 pts — **Sprint 2**

### US10 — Ver tempo total parado por equipamento
**Como** Gerente de Produção, **eu quero** ver o tempo total parado de cada equipamento em um período selecionado, **para** identificar os equipamentos mais problemáticos (RF07).

**Estimativa:** 3 pts — **Sprint 2**

---

## E4 — Fundação Técnica (enablers, sem persona de negócio)

| ID | Item técnico | Estimativa | Sprint |
|---|---|---|---|
| T01 | Setup do projeto Spring Boot (estrutura modular, Maven, Flyway, conexão PostgreSQL) | 3 pts | 1 |
| T02 | Spring Security com perfis de acesso (RN04) | 5 pts | 1 |
| T03 | Configuração do Swagger/OpenAPI | 2 pts | 1 |
| T04 | Canal WebSocket (STOMP) para notificação em tempo real de novas paradas | 5 pts | 2 |
| T05 | Suite de testes automatizados das regras de negócio RN01-RN04 (JUnit + Testcontainers) | 5 pts | 1-2 |

---

## Sprint Planning

### Sprint 1 — Fundação + registro básico (19 pts)
T01, T02, T03, US01, US02, US04, US05, US06

**Sprint Goal:** *"Um operador consegue abrir e encerrar uma parada de máquina real, com dados de equipamento e motivo previamente cadastrados, respeitando as regras de negócio básicas."*

### Sprint 2 — Consulta, correção e tempo real (21 pts)
US03, US08, US09, US10, T04, T05

**Sprint Goal:** *"Supervisores e gerentes conseguem consultar, corrigir e visualizar indicadores de paradas, com atualização em tempo real no dashboard."*

---

## Definition of Done (DoD) — vale para todas as stories
- [ ] Código implementado e revisado (code review)
- [ ] Testes automatizados cobrindo as regras de negócio da story
- [ ] Endpoint documentado no Swagger
- [ ] Migração Flyway versionada (quando houver mudança de schema)
- [ ] Validado manualmente contra os critérios de aceite (Gherkin) da story
