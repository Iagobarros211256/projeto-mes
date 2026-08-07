package com.biscoitos.manutencao.paradas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T05 — testes de integração contra Postgres real via Testcontainers, não Mockito.
 *
 * Esta suíte existe porque DUAS classes de bug real neste projeto nunca teriam sido
 * pegas por teste unitário com mock:
 *   1. LazyInitializationException em GET /api/equipamentos e /api/paradas (Sprint 1) —
 *      só aparece quando existe uma sessão Hibernate de verdade fechando fora de hora.
 *   2. Erro de inferência de tipo do driver Postgres em parâmetros de data opcionais
 *      (US08) — só aparece contra um Postgres de verdade, nunca contra um mock.
 * Os dois só foram encontrados porque testamos manualmente contra banco real. Esta
 * suíte formaliza isso, pra não depender de alguém lembrar de repetir o teste manual
 * toda vez que mexer nessas áreas do código.
 *
 * @ServiceConnection (Spring Boot 3.1+) configura o datasource pro container
 * automaticamente — não precisa de @DynamicPropertySource manual.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ParadaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String OPERADOR = "ana.operadora@trigodourado.com.br";
    private static final String GESTOR = "diego.gestor@trigodourado.com.br";
    private static final String SENHA = "senha123";

    @Test
    void deveListarEquipamentosSemLazyInitializationException() throws Exception {
        // Regressão do bug real do Sprint 1. Se o JOIN FETCH em
        // EquipamentoRepository.findAllComLinhaProducao() for removido por engano
        // no futuro, este teste quebra com 500, não com uma asserção de valor errado.
        mockMvc.perform(get("/api/equipamentos").with(httpBasic(OPERADOR, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].linhaProducaoNome").value("Linha 1"));
    }

    @Test
    void deveAbrirEncerrarConsultarEFiltrarParada() throws Exception {
        UUID equipamentoId = equipamentoIdPorCodigo("FORNO-01");
        UUID motivoId = primeiroMotivoId();

        String corpoAbrir = """
                {"equipamentoId": "%s", "motivoId": "%s", "observacoes": "Teste de integração"}
                """.formatted(equipamentoId, motivoId);

        String respostaAbrir = mockMvc.perform(post("/api/paradas")
                        .with(httpBasic(OPERADOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAbrir))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andReturn().getResponse().getContentAsString();

        UUID paradaId = UUID.fromString(objectMapper.readTree(respostaAbrir).get("id").asText());

        // RN02 — segunda parada no MESMO equipamento tem que dar 409.
        mockMvc.perform(post("/api/paradas")
                        .with(httpBasic(OPERADOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAbrir))
                .andExpect(status().isConflict());

        // Encerrar de verdade.
        mockMvc.perform(patch("/api/paradas/" + paradaId + "/encerrar")
                        .with(httpBasic(OPERADOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.duracaoMinutos").isNumber());

        // RN01 — reencerrar uma parada já ENCERRADA tem que dar 409.
        mockMvc.perform(patch("/api/paradas/" + paradaId + "/encerrar")
                        .with(httpBasic(OPERADOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());

        // US08 — regressão do filtro que quebrou DUAS VEZES contra Postgres real antes
        // da Specification (ver ParadaSpecifications). Se alguém reintroduzir o padrão
        // "(:param IS NULL OR ...)" em JPQL puro, este teste é o primeiro a acusar.
        mockMvc.perform(get("/api/paradas")
                        .param("equipamentoId", equipamentoId.toString())
                        .param("dataInicio", "2020-01-01T00:00:00Z")
                        .with(httpBasic(OPERADOR, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(paradaId.toString()));

        // US10 — a mesma parada precisa aparecer na agregação de tempo parado.
        mockMvc.perform(get("/api/paradas/tempo-parado").with(httpBasic(OPERADOR, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.equipamentoId == '" + equipamentoId + "')]").exists());
    }

    @Test
    void deveRecusarGestorAbrirParada_RN04() throws Exception {
        UUID equipamentoId = equipamentoIdPorCodigo("EMPAC-01");
        UUID motivoId = primeiroMotivoId();

        String corpo = """
                {"equipamentoId": "%s", "motivoId": "%s"}
                """.formatted(equipamentoId, motivoId);

        mockMvc.perform(post("/api/paradas")
                        .with(httpBasic(GESTOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarRequisicaoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/paradas"))
                .andExpect(status().isUnauthorized());
    }

    private UUID equipamentoIdPorCodigo(String codigo) throws Exception {
        String resposta = mockMvc.perform(get("/api/equipamentos").with(httpBasic(OPERADOR, SENHA)))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode node : objectMapper.readTree(resposta)) {
            if (node.get("codigo").asText().equals(codigo)) {
                return UUID.fromString(node.get("id").asText());
            }
        }
        throw new IllegalStateException("Equipamento " + codigo + " não encontrado no seed (V4)");
    }

    private UUID primeiroMotivoId() throws Exception {
        String resposta = mockMvc.perform(get("/api/motivos-parada").with(httpBasic(OPERADOR, SENHA)))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(resposta).get(0).get("id").asText());
    }
}
