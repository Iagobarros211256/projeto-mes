# Modelo de Dados — Módulo de Paradas de Máquina

## Entidades

### linha_producao (core)
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| nome | VARCHAR | |
| descricao | VARCHAR | |

### equipamento (core)
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| linha_producao_id | UUID (FK) | |
| codigo | VARCHAR | |
| nome | VARCHAR | |
| setor | VARCHAR | |
| status | VARCHAR | |
| horas_operacao_acumuladas | INT | Preparado para o módulo de Preventiva (Fase 2) |

### motivo_parada
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| categoria | VARCHAR | Ex: Mecânica, Elétrica, Falta de Material, Setup, Limpeza, Qualidade |
| subcategoria | VARCHAR | |
| descricao | VARCHAR | |

### usuario (core)
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| nome | VARCHAR | |
| email | VARCHAR | |
| perfil | VARCHAR | OPERADOR / TECNICO / SUPERVISOR / GESTOR / GERENTE |

### parada
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| equipamento_id | UUID (FK) | |
| motivo_id | UUID (FK) | |
| responsavel_id | UUID (FK -> usuario) | |
| ordem_servico_id | UUID (FK, nullable) | Preparado para o módulo de OS (Fase 2) |
| data_hora_inicio | TIMESTAMP | |
| data_hora_fim | TIMESTAMP (nullable) | Nulo enquanto a parada está em aberto |
| duracao_minutos | INT (nullable, calculado) | Persistido no encerramento (RF03) |
| observacoes | TEXT | |
| status | VARCHAR | ABERTA / ENCERRADA |

### log_auditoria (core, genérico)
| Campo | Tipo | Observação |
|---|---|---|
| id | UUID (PK) | |
| entidade | VARCHAR | Nome da tabela auditada |
| entidade_id | UUID | |
| usuario_id | UUID (FK) | |
| acao | VARCHAR | CREATE / UPDATE / DELETE |
| data_hora | TIMESTAMP | |

## Relacionamentos
- linha_producao 1 --- N equipamento
- equipamento 1 --- N parada
- motivo_parada 1 --- N parada
- usuario 1 --- N parada (responsavel)
- usuario 1 --- N log_auditoria

## Regras de modelagem
1. RN01/RN02 (parada não pode ter fim < início; não pode haver duas paradas abertas no mesmo equipamento) serão garantidas na camada de serviço (não dá para expressar via constraint simples de banco sem trigger).
2. duracao_minutos é uma coluna calculada e persistida no momento do encerramento, não um campo virtual, para preservar o histórico caso a lógica de cálculo mude.
3. ordem_servico_id já nasce nullable para evitar ALTER TABLE quando o módulo de OS for implementado.
