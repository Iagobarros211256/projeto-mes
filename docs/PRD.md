# PRD — Sistema de Gestão de Manutenção e Produção
## Cliente: Biscoitos Trigo Dourado Ltda.

| Campo | Valor |
|---|---|
| Versão | 0.1 (Discovery) |
| Data | 13/07/2026 |
| Autor | Equipe de Desenvolvimento (Claude) |
| Status | Aguardando validação do cliente |
| Stakeholder | Iago (Product Owner / Cliente) |

---

## 1. Contexto e Justificativa de Negócio

A **Biscoitos Trigo Dourado** é uma fábrica de médio porte, com uma planta industrial contendo **3 linhas de produção**, cada uma composta pelas seguintes estações:

1. Misturadora de massa
2. Modeladora/Laminadora
3. Forno túnel (esteira contínua)
4. Resfriador (túnel de resfriamento)
5. Empacotadora
6. Encaixotadora / Paletizadora

Atualmente, o controle de paradas de máquina, ordens de manutenção e estoque de peças é feito em **planilhas Excel isoladas**, sem histórico confiável, sem cálculo de indicadores (OEE) e sem rastreabilidade de causas de parada. Isso gera:

- Perda de informação sobre motivos reais de parada;
- Dificuldade em priorizar manutenção preventiva;
- Falta de visibilidade gerencial sobre disponibilidade das linhas;
- Risco de ruptura de estoque de peças críticas.

O projeto visa substituir esse controle manual por um **sistema informatizado único**, integrando os 5 módulos abaixo.

---

## 2. Visão do Produto

> Centralizar o registro e a análise de eventos de manutenção e produção da fábrica, permitindo que operadores registrem paradas em tempo real, técnicos gerenciem ordens de serviço, e gestores tomem decisões baseadas em indicadores confiáveis (OEE, disponibilidade, MTTR/MTBF).

---

## 3. Stakeholders e Personas

| Persona | Papel | Necessidade principal |
|---|---|---|
| **Operador de Linha** | Opera a máquina no chão de fábrica | Registrar parada rapidamente (poucos cliques, tablet/terminal) |
| **Técnico de Manutenção** | Executa reparos | Ver ordens abertas, atualizar status, apontar peças usadas |
| **Supervisor de Produção** | Gerencia turno | Ver paradas do turno, abrir OS a partir de uma parada |
| **Gestor de Manutenção (PCM)** | Planeja preventivas | Calendário de manutenção, alertas de revisão por horas de uso |
| **Gerente de Produção/Industrial** | Decisão gerencial | Dashboard de OEE, causas raiz, disponibilidade |
| **Almoxarife** | Controla peças | Entrada/saída de estoque, alertas de estoque mínimo |

---

## 4. Escopo Geral do Sistema (visão macro dos 5 módulos)

| # | Módulo | Resumo | Fase |
|---|---|---|---|
| 1 | **Registro de Paradas de Máquina** | Motivo, duração, equipamento, responsável | **MVP — Sprint 1-2** |
| 2 | Ordens de Serviço (OS) | Abertura, acompanhamento, encerramento de manutenções | Fase 2 |
| 3 | Manutenção Preventiva | Calendário, horas de operação, alertas de revisão | Fase 2 |
| 4 | Dashboard de Produção (OEE) | OEE, tempo parado, disponibilidade, causas | Fase 3 |
| 5 | Estoque de Peças | Entrada, saída, estoque mínimo, histórico | Fase 3 |

**Racional da priorização:** Paradas de Máquina foi escolhido como MVP porque é o módulo que **gera o dado bruto** que alimenta praticamente todos os outros (uma OS geralmente nasce de uma parada; o Dashboard de OEE depende do histórico de paradas; a preventiva usa o histórico de falhas para ajustar calendário). Começar por aqui é a base de dados fundacional do sistema.

---

## 5. MVP — Módulo: Registro de Paradas de Máquina

### 5.1 Objetivo do Módulo
Permitir o registro estruturado de cada parada de máquina ocorrida na linha de produção, com motivo padronizado, duração calculada automaticamente, equipamento afetado e responsável pelo registro/atendimento.

### 5.2 Requisitos Funcionais

| ID | Descrição |
|---|---|
| RF01 | O sistema deve permitir cadastrar equipamentos/máquinas (código, nome, linha, setor) |
| RF02 | O sistema deve permitir abrir um registro de parada, informando: equipamento, data/hora de início, motivo (categoria + subcategoria) e responsável |
| RF03 | O sistema deve permitir encerrar a parada, calculando automaticamente a duração (hora fim − hora início) |
| RF04 | O sistema deve possuir uma taxonomia de motivos de parada pré-cadastrada (ex.: Mecânica, Elétrica, Falta de Material, Setup/Troca de Formato, Limpeza, Qualidade) com subcategorias |
| RF05 | O sistema deve permitir consultar o histórico de paradas com filtros (por equipamento, por período, por motivo, por responsável) |
| RF06 | O sistema deve permitir editar/corrigir um apontamento (com log de quem alterou) |
| RF07 | O sistema deve exibir, para cada equipamento, o tempo total parado no período selecionado |
| RF08 | O sistema deve permitir vincular futuramente uma parada a uma Ordem de Serviço (campo preparado, mesmo que o módulo de OS ainda não exista) |

### 5.3 Regras de Negócio

- RN01: Uma parada não pode ser encerrada com hora de fim anterior à hora de início.
- RN02: Um equipamento não pode ter duas paradas "em aberto" simultaneamente.
- RN03: Toda parada deve obrigatoriamente ter uma categoria de motivo (não é permitido parada "sem motivo").
- RN04: Apenas usuários com perfil Operador, Supervisor ou Técnico podem registrar paradas; Gestor e Gerente têm acesso somente leitura neste módulo.

### 5.4 Requisitos Não-Funcionais

- RNF01: Interface de abertura de parada deve ser utilizável em tablet/terminal de chão de fábrica (poucos campos, resposta rápida).
- RNF02: Tempo de resposta da API < 300ms para operações de escrita simples.
- RNF03: Toda alteração deve manter trilha de auditoria (quem, quando, o quê).
- RNF04: Sistema deve suportar múltiplos usuários simultâneos por linha de produção.

---

## 6. ADR-001 — Decisão de Arquitetura e Stack Tecnológica

**Status:** ✅ **Aprovado pelo cliente em 13/07/2026**

**Contexto:** Cliente exigiu Java no backend. Demais camadas definidas em conjunto durante o discovery.

**Decisão final:**

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Backend | **Java 21 + Spring Boot 3** | Stack Java corporativa padrão de mercado; Spring Data JPA acelera modelagem; Spring Security cobre RN04 (perfis de acesso) |
| Build | **Maven** | Padrão de mercado, integração nativa com Spring Initializr |
| Banco de Dados | **PostgreSQL** | Robusto para relacional transacional, gratuito, ótimo suporte a agregações (necessárias para o futuro Dashboard OEE) |
| Frontend | **React + TypeScript** | SPA desacoplada da API REST, permite evoluir para telas de chão de fábrica (tablet) e dashboards gerenciais com a mesma base |
| Migrações de banco | **Flyway** | Versionamento de schema junto com o código, prática padrão em projetos Spring |

**Arquitetura macro:** **Monólito Modular** — aplicação única, organizada em pacotes por domínio (`paradas`, `os`, `preventiva`, `dashboard`, `estoque`, `core`), cada um com camadas internas próprias. Escolhido em vez de microsserviços (complexidade desproporcional ao tamanho do projeto) e em vez de monólito tradicional sem separação (não escalaria bem para 5 módulos).

**Protocolo de comunicação:**
| Protocolo | Uso |
|---|---|
| **REST (JSON) + OpenAPI/Swagger** | Canal principal — todas as operações de CRUD (abrir/encerrar parada, cadastrar equipamento, etc.) |
| **WebSocket (STOMP)** | Canal complementar — eventos em tempo real (ex.: parada registrada aparece instantaneamente no dashboard/supervisor, sem polling) |
| gRPC | Descartado por ora — só faria sentido se um módulo fosse extraído futuramente como microsserviço separado |

**Alternativas consideradas e descartadas:**
- Frontend server-side (Thymeleaf): descartado por limitar a evolução futura do Dashboard de OEE, que exigirá gráficos ricos e interatividade.
- Banco NoSQL: descartado — o domínio é fortemente relacional (equipamento → parada → OS → peça), com necessidade de integridade referencial.
- Microsserviços desde o início: descartado — overhead de orquestração, service discovery e transações distribuídas não se justifica no estágio atual do projeto.
- WebSocket para 100% da API: descartado — perderia autenticação/ferramentas padrão do ecossistema REST (Swagger, Postman) sem necessidade real, já que a maior parte das operações é request/response simples.

---

## 7. Fora do Escopo do MVP (backlog futuro)

- Módulos de OS, Preventiva, Dashboard OEE e Estoque (Fases 2 e 3, conforme seção 4)
- Autenticação via SSO/LDAP corporativo (MVP terá login simples)
- Notificações por e-mail/push
- Aplicativo mobile nativo (MVP será web responsivo)

---

## 8. Critérios de Aceite do MVP (Sprint 1-2)

- [ ] CRUD de equipamentos funcional
- [ ] Abertura e encerramento de parada com cálculo automático de duração
- [ ] Taxonomia de motivos parametrizável
- [ ] Listagem de paradas com filtros
- [ ] Regras de negócio RN01–RN04 validadas por testes automatizados
- [ ] API documentada via Swagger

---

## 9. Próximos Passos

1. **Validação deste PRD** pelo cliente (você) — especialmente ADR-001.
2. Modelagem de dados (diagrama ER) do módulo de Paradas.
3. Quebra em **User Stories** e **Backlog do Sprint 1**.
4. Diagrama de arquitetura (componentes).
5. Início da implementação (Sprint 1).
