# Ideia futura — Integração de IA local (Ollama) no projeto

> Status: **ideia registrada, não é backlog fechado ainda**. Discutido numa conversa
> separada ("Integrating Ollama AI into the project", 2026-09-11) que parou logo no
> início, aguardando decisões que nunca foram respondidas. Trazido pra cá pra não se
> perder e pra considerar encaixar numa sprint futura.

## Por que Ollama (e não uma API de nuvem)

O Iago já tem Ollama instalado localmente pra rodar LLMs (ver perfil). Usar isso no
projeto significa custo zero de API e dado nunca saindo da máquina — bom pra um projeto
de portfólio que já leva segurança e RBAC a sério.

## Peça técnica: Spring AI

Quem faz o trabalho pesado é o **Spring AI**, que tem starter oficial de Ollama e se
integra direto na stack (Java 21 + Spring Boot 3). Ponto de atenção: Spring AI 2.0 já
saiu, mas acompanha Spring Boot 4 — como o projeto é Boot 3.3.4, a versão certa é a
linha **1.1.x**.

```xml
<properties>
    <spring-ai.version>1.1.0</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>
</dependencies>
```

`application.yml`:
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      init:
        pull-model-strategy: never   # assume que o modelo já foi puxado manualmente
      chat:
        options:
          model: qwen3:8b
          temperature: 0.1           # extração/classificação quer determinismo, não criatividade
      embedding:
        options:
          model: nomic-embed-text
```

Com o starter no classpath, o Spring AI injeta um `ChatClient.Builder` já configurado —
o código da aplicação fica agnóstico de provedor (trocar por nuvem depois é mudar
dependência/config, não código).

## As três ideias de caso de uso, por ordem de custo/retorno

1. **Extração estruturada** — relato livre do operador (texto solto sobre o que
   aconteceu numa parada) → JSON estruturado com equipamento, tipo de falha, severidade.
   **Menor esforço, maior retorno**: é só um `ChatClient` + `.entity(RelatoFalha.class)`,
   sem infraestrutura nova. O Ollama tem API de Structured Outputs que força a resposta a
   obedecer um JSON Schema, garantindo o formato.

2. **Resumo de turno / relatório de downtime** — também barato, encaixa direto no módulo
   de Paradas que já existe (gera um resumo em texto a partir do histórico de paradas de
   um turno/período). Precisa de cuidado com prompt e com o volume de dado que entra no
   contexto.

3. **Busca semântica com embeddings + pgvector** — a mais cara das três. Exige extensão
   no Postgres, migration Flyway nova, `VectorStore`, pipeline de indexação e
   reindexação. Vale a pena, mas como sprint separada, não junto com as outras duas.

## Decisão de arquitetura já esboçada (vale manter se isso avançar)

No monólito modular, chamada de LLM **não deveria morar dentro dos módulos de domínio**
(paradas, ordemservico, etc.). A ideia era um módulo `ai` (ou `assistencia`) exposto por
interface, com os módulos de negócio chamando essa porta — assim o domínio não fica
acoplado ao Spring AI, e testes unitários de OS/Paradas não precisam do Ollama rodando
pra passar. Pros testes de integração, o Spring AI tem módulo próprio de Testcontainers,
que encaixaria no padrão de T05/T07/T11 que já usamos.

## Perguntas que ficaram em aberto (seriam o kickoff se isso virar sprint de verdade)

1. Qual caso de uso atacar primeiro — extração estruturada, resumo de turno, ou busca semântica?
2. Quanto de VRAM/RAM tem a máquina onde o Ollama roda (afeta qual tamanho de modelo é viável)?
3. Preferência de entrega — PRD+ADR antes do código, arquivo por arquivo, ou tudo de uma vez?

## Onde isso encaixaria no roadmap

Depois do Dashboard de OEE (Sprint 6), como um **Sprint 7 dedicado** — não misturar com
outro sprint, já que introduz uma dependência de infraestrutura nova (Ollama rodando) que
os outros módulos não têm.
