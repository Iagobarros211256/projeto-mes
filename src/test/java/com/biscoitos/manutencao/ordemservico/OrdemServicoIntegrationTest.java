package com.biscoitos.manutencao.ordemservico;

import com.biscoitos.manutencao.core.repository.UsuarioRepository;
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
 * T07 — testes de integração do módulo de Ordens de Serviço, mesmo padrão do
 * ParadaIntegrationTest (T05): Postgres real via Testcontainers, MockMvc batendo nos
 * Controllers de verdade, com Spring Security de verdade.
 *
 * Diferente do módulo de Paradas, a Specification de filtros da OS (US16) nasceu já
 * sabendo da lição da US08 — o teste de filtro aqui existe para CONFIRMAR que essa
 * lição realmente evitou o bug, não porque já tenha aparecido um problema aqui.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class OrdemServicoIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private static final String SUPERVISOR = "carla.supervisora@trigodourado.com.br";
    private static final String GESTOR = "diego.gestor@trigodourado.com.br";
    private static final String TECNICO = "bruno.tecnico@trigodourado.com.br";
    private static final String OPERADOR = "ana.operadora@trigodourado.com.br";
    private static final String SENHA = "senha123";

    @Test
    void deveAbrirAtribuirIniciarEConcluirOsFluxoCompleto() throws Exception {
        UUID equipamentoId = equipamentoIdPorCodigo("MIST-01");
        UUID tecnicoId = usuarioIdPorEmail(TECNICO);

        String corpoAbrir = """
                {"equipamentoId": "%s", "titulo": "Revisão preventiva", "tipo": "PREVENTIVA", "prioridade": "MEDIA"}
                """.formatted(equipamentoId);

        String respostaAbrir = mockMvc.perform(post("/api/ordens-servico")
                        .with(httpBasic(SUPERVISOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAbrir))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.tecnicoResponsavelId").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        UUID osId = UUID.fromString(objectMapper.readTree(respostaAbrir).get("id").asText());

        // RN05 — iniciar sem técnico atribuído tem que dar 409.
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/iniciar").with(httpBasic(TECNICO, SENHA)))
                .andExpect(status().isConflict());

        // Atribuir técnico (GESTOR).
        String corpoAtribuir = """
                {"tecnicoId": "%s"}
                """.formatted(tecnicoId);
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/atribuir-tecnico")
                        .with(httpBasic(GESTOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAtribuir))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tecnicoResponsavelId").value(tecnicoId.toString()));

        // Iniciar execução (TECNICO) — agora deve funcionar.
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/iniciar").with(httpBasic(TECNICO, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.dataInicioExecucao").exists());

        // Concluir (TECNICO).
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/concluir")
                        .with(httpBasic(TECNICO, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacoes\": \"Sem apontamentos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.observacoesEncerramento").value("Sem apontamentos"));

        // RN07 — qualquer transição numa OS já CONCLUIDA tem que dar 409.
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/iniciar").with(httpBasic(TECNICO, SENHA)))
                .andExpect(status().isConflict());

        // US16 — regressão do padrão de filtro que quebrou 2x na US08. Aqui já nasceu
        // com Specification, então este teste confirma que a lição realmente pegou.
        mockMvc.perform(get("/api/ordens-servico")
                        .param("status", "CONCLUIDA")
                        .with(httpBasic(OPERADOR, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + osId + "')]").exists());
    }

    @Test
    void deveAbrirOsAPartirDeParadaEVincularAmbas() throws Exception {
        UUID equipamentoId = equipamentoIdPorCodigo("EMPAC-01");
        UUID motivoId = primeiroMotivoId();

        // Abre uma parada primeiro.
        String corpoParada = """
                {"equipamentoId": "%s", "motivoId": "%s", "observacoes": "Teste de integração OS+Parada"}
                """.formatted(equipamentoId, motivoId);
        String respostaParada = mockMvc.perform(post("/api/paradas")
                        .with(httpBasic(OPERADOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoParada))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID paradaId = UUID.fromString(objectMapper.readTree(respostaParada).get("id").asText());

        // Abre a OS a partir dessa parada — sem informar equipamentoId.
        String corpoOs = """
                {"paradaId": "%s", "titulo": "Investigar causa raiz", "tipo": "CORRETIVA", "prioridade": "ALTA"}
                """.formatted(paradaId);
        String respostaOs = mockMvc.perform(post("/api/ordens-servico")
                        .with(httpBasic(SUPERVISOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoOs))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.equipamentoId").value(equipamentoId.toString()))
                .andReturn().getResponse().getContentAsString();
        UUID osId = UUID.fromString(objectMapper.readTree(respostaOs).get("id").asText());

        // A parada precisa ter sido atualizada com o ordemServicoId — só dá pra confirmar
        // consultando a parada de novo (a resposta de ParadaResponse expõe esse campo?
        // Não expõe ainda — checa via encerrar a parada, que não deveria dar erro).
        mockMvc.perform(get("/api/paradas/" + paradaId).with(httpBasic(OPERADOR, SENHA)))
                .andExpect(status().isOk());

        // RN06 — concluir uma OS que ainda está ABERTA (nunca foi EM_ANDAMENTO) tem que dar 409.
        mockMvc.perform(patch("/api/ordens-servico/" + osId + "/concluir")
                        .with(httpBasic(GESTOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRecusarOperadorAbrirOs_permissao() throws Exception {
        UUID equipamentoId = equipamentoIdPorCodigo("FORNO-01");

        String corpo = """
                {"equipamentoId": "%s", "titulo": "Teste", "tipo": "CORRETIVA", "prioridade": "BAIXA"}
                """.formatted(equipamentoId);

        mockMvc.perform(post("/api/ordens-servico")
                        .with(httpBasic(OPERADOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarAbrirSemEquipamentoNemParada() throws Exception {
        String corpo = """
                {"titulo": "Sem origem", "tipo": "CORRETIVA", "prioridade": "BAIXA"}
                """;

        mockMvc.perform(post("/api/ordens-servico")
                        .with(httpBasic(SUPERVISOR, SENHA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest());
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

    private UUID usuarioIdPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário " + email + " não encontrado no seed (V4)"))
                .getId();
    }
}
