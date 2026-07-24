# manutencao-backend

Sistema de Gestão de Manutenção e Produção — Biscoitos Trigo Dourado Ltda.
Sprint 1: módulo de **Registro de Paradas de Máquina** (MVP).

## Pré-requisitos

- JDK 21
- Maven 3.9+
- Docker (para o Postgres local) — ou um Postgres 16 já rodando na sua máquina

## Como rodar localmente

```bash
# 1. Subir o banco
docker compose up -d

# 2. Rodar a aplicação (o Flyway aplica as migrations automaticamente no start)
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`. Documentação interativa (Swagger) em
`http://localhost:8080/swagger-ui.html`.

> **Nota:** este projeto foi escrito neste ambiente sem acesso ao Maven Central,
> então nunca foi compilado de fato aqui — foi revisado com cuidado, mas rode
> `mvn clean verify` na sua máquina antes de considerar o Sprint 1 encerrado.

## Autenticação (MVP)

HTTP Basic. Usuários de desenvolvimento (seed em `V4__seed_dados_desenvolvimento.sql`),
todos com senha `senha123`:

| Email | Perfil | Pode abrir/encerrar parada? |
|---|---|---|
| ana.operadora@trigodourado.com.br | OPERADOR | Sim |
| bruno.tecnico@trigodourado.com.br | TECNICO | Sim |
| carla.supervisora@trigodourado.com.br | SUPERVISOR | Sim |
| diego.gestor@trigodourado.com.br | GESTOR | Não (só leitura + cadastros) |
| elisa.gerente@trigodourado.com.br | GERENTE | Não (só leitura) |

## Testando o fluxo principal (US05 + US06) via curl

```bash
# Listar equipamentos (para pegar um equipamentoId)
curl -u ana.operadora@trigodourado.com.br:senha123 http://localhost:8080/api/equipamentos

# Listar motivos de parada (para pegar um motivoId)
curl -u ana.operadora@trigodourado.com.br:senha123 http://localhost:8080/api/motivos-parada

# Abrir uma parada
curl -u ana.operadora@trigodourado.com.br:senha123 \
  -X POST http://localhost:8080/api/paradas \
  -H "Content-Type: application/json" \
  -d '{"equipamentoId": "<uuid>", "motivoId": "<uuid>", "observacoes": "Ruído estranho na esteira"}'

# Encerrar a parada
curl -u ana.operadora@trigodourado.com.br:senha123 \
  -X PATCH http://localhost:8080/api/paradas/<paradaId>/encerrar \
  -H "Content-Type: application/json" \
  -d '{}'
```

## Estrutura do projeto

Ver `docs/modelo-dados.md` e o diagrama de componentes gerado durante o discovery
para o racional da organização em `core` (compartilhado) e `paradas` (módulo do MVP).

## Rodando os testes

```bash
mvn test
```
