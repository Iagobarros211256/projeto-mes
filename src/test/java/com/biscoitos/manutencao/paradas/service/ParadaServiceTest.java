package com.biscoitos.manutencao.paradas.service;

import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.paradas.domain.MotivoParada;
import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.domain.StatusParada;
import com.biscoitos.manutencao.paradas.repository.MotivoParadaRepository;
import com.biscoitos.manutencao.paradas.repository.ParadaRepository;
import com.biscoitos.manutencao.paradas.repository.TempoParadoPorEquipamento;
import com.biscoitos.manutencao.paradas.service.exception.IntervaloInvalidoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaEmAbertoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaJaEncerradaException;
import com.biscoitos.manutencao.paradas.web.dto.AbrirParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.EditarParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.EncerrarParadaRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unitário, com Mockito — cobre as regras de negócio do ParadaService isoladamente.
 * A suíte de integração com Testcontainers (batendo em Postgres real, validando as
 * migrations e os índices) é item T05 do Sprint 2.
 */
@ExtendWith(MockitoExtension.class)
class ParadaServiceTest {

    @Mock
    private ParadaRepository paradaRepository;
    @Mock
    private EquipamentoRepository equipamentoRepository;
    @Mock
    private MotivoParadaRepository motivoParadaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private com.biscoitos.manutencao.core.service.AuditoriaService auditoriaService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ParadaService paradaService;

    @Test
    void deveRecusarAbrirParadaQuandoEquipamentoJaTemParadaEmAberto() {
        UUID equipamentoId = UUID.randomUUID();
        UUID motivoId = UUID.randomUUID();
        UUID responsavelId = UUID.randomUUID();

        when(equipamentoRepository.findById(equipamentoId))
                .thenReturn(Optional.of(Equipamento.builder().id(equipamentoId).build()));
        when(motivoParadaRepository.findById(motivoId))
                .thenReturn(Optional.of(MotivoParada.builder().id(motivoId).build()));
        when(usuarioRepository.findById(responsavelId))
                .thenReturn(Optional.of(Usuario.builder().id(responsavelId).build()));
        when(paradaRepository.existsByEquipamentoIdAndStatus(equipamentoId, StatusParada.ABERTA))
                .thenReturn(true);

        AbrirParadaRequest request = new AbrirParadaRequest(equipamentoId, motivoId, "obs");

        // RN02
        assertThatThrownBy(() -> paradaService.abrirParada(request, responsavelId))
                .isInstanceOf(ParadaEmAbertoException.class);
    }

    @Test
    void devePermitirAbrirParadaQuandoEquipamentoEstaLivre() {
        UUID equipamentoId = UUID.randomUUID();
        UUID motivoId = UUID.randomUUID();
        UUID responsavelId = UUID.randomUUID();

        when(equipamentoRepository.findById(equipamentoId))
                .thenReturn(Optional.of(Equipamento.builder().id(equipamentoId).build()));
        when(motivoParadaRepository.findById(motivoId))
                .thenReturn(Optional.of(MotivoParada.builder().id(motivoId).build()));
        when(usuarioRepository.findById(responsavelId))
                .thenReturn(Optional.of(Usuario.builder().id(responsavelId).build()));
        when(paradaRepository.existsByEquipamentoIdAndStatus(equipamentoId, StatusParada.ABERTA))
                .thenReturn(false);
        when(paradaRepository.save(any(Parada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AbrirParadaRequest request = new AbrirParadaRequest(equipamentoId, motivoId, "Ruído na esteira");

        Parada parada = paradaService.abrirParada(request, responsavelId);

        assertThat(parada.getStatus()).isEqualTo(StatusParada.ABERTA);
        assertThat(parada.getDataHoraInicio()).isNotNull();
        assertThat(parada.getObservacoes()).isEqualTo("Ruído na esteira");
        // T04 — confirma que o evento de tempo real foi publicado (o envio de fato pro
        // STOMP é responsabilidade do ParadaEventoListener, testado à parte se necessário;
        // aqui só garantimos que o Service publica o evento certo no momento certo).
        verify(eventPublisher).publishEvent(any(com.biscoitos.manutencao.paradas.event.ParadaEventoTempoReal.class));
    }

    @Test
    void deveRecusarEncerrarParadaComFimAnteriorAoInicio() {
        UUID paradaId = UUID.randomUUID();
        Instant inicio = Instant.now();

        Parada parada = Parada.builder()
                .id(paradaId)
                .dataHoraInicio(inicio)
                .status(StatusParada.ABERTA)
                .build();

        when(paradaRepository.buscarComRelacionamentosPorId(paradaId)).thenReturn(Optional.of(parada));

        EncerrarParadaRequest request = new EncerrarParadaRequest(inicio.minus(1, ChronoUnit.MINUTES));

        // RN01
        assertThatThrownBy(() -> paradaService.encerrarParada(paradaId, request))
                .isInstanceOf(IntervaloInvalidoException.class);
    }

    @Test
    void deveCalcularDuracaoCorretamenteAoEncerrar() {
        UUID paradaId = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-07-20T10:00:00Z");
        Instant fim = Instant.parse("2026-07-20T10:45:00Z");

        Parada parada = Parada.builder()
                .id(paradaId)
                .dataHoraInicio(inicio)
                .status(StatusParada.ABERTA)
                // Necessário desde o T04: encerrarParada monta um ParadaResponse
                // internamente (pro evento de tempo real), que acessa esses três campos.
                .equipamento(Equipamento.builder().id(UUID.randomUUID()).nome("Equip. Teste").build())
                .motivo(MotivoParada.builder().id(UUID.randomUUID()).categoria("Mecânica").build())
                .responsavel(Usuario.builder().id(UUID.randomUUID()).nome("Usuário Teste").build())
                .build();

        when(paradaRepository.buscarComRelacionamentosPorId(paradaId)).thenReturn(Optional.of(parada));
        when(paradaRepository.save(any(Parada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Parada encerrada = paradaService.encerrarParada(paradaId, new EncerrarParadaRequest(fim));

        assertThat(encerrada.getStatus()).isEqualTo(StatusParada.ENCERRADA);
        assertThat(encerrada.getDuracaoMinutos()).isEqualTo(45L);
        verify(eventPublisher).publishEvent(any(com.biscoitos.manutencao.paradas.event.ParadaEventoTempoReal.class));
    }

    @Test
    void deveRecusarEncerrarParadaJaEncerrada() {
        UUID paradaId = UUID.randomUUID();
        Parada parada = Parada.builder()
                .id(paradaId)
                .dataHoraInicio(Instant.now().minusSeconds(3600))
                .status(StatusParada.ENCERRADA)
                .build();

        when(paradaRepository.buscarComRelacionamentosPorId(paradaId)).thenReturn(Optional.of(parada));

        assertThatThrownBy(() -> paradaService.encerrarParada(paradaId, new EncerrarParadaRequest(Instant.now())))
                .isInstanceOf(ParadaJaEncerradaException.class);
    }

    @Test
    void deveFalharAoAbrirParadaComEquipamentoInexistente() {
        UUID equipamentoId = UUID.randomUUID();
        when(equipamentoRepository.findById(equipamentoId)).thenReturn(Optional.empty());

        AbrirParadaRequest request = new AbrirParadaRequest(equipamentoId, UUID.randomUUID(), null);

        assertThatThrownBy(() -> paradaService.abrirParada(request, UUID.randomUUID()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void deveDelegarFiltrosParaORepositoryComOrdenacaoPorInicio() {
        // US08. Este teste garante que o Service chama findAll com uma Specification
        // e a ordenação certa — não valida o CONTEÚDO do filtro em si, porque Mockito não
        // executa a query de verdade. A lógica real do filtro (Criteria/SQL) só um teste
        // de integração contra Postgres real valida, que é o T05, ainda no backlog.
        Sort ordenacaoEsperada = Sort.by(Sort.Direction.DESC, "dataHoraInicio");
        List<Parada> esperado = List.of(Parada.builder().id(UUID.randomUUID()).build());

        when(paradaRepository.findAll(any(Specification.class), eq(ordenacaoEsperada)))
                .thenReturn(esperado);

        List<Parada> resultado = paradaService.listarComFiltros(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-07-31T23:59:59Z"));

        assertThat(resultado).isEqualTo(esperado);
        verify(paradaRepository).findAll(any(Specification.class), eq(ordenacaoEsperada));
    }

    @Test
    void deveAceitarTodosOsFiltrosNulos() {
        // Sem filtro nenhum = listar tudo. Mesmo caminho de código do teste acima —
        // existe como teste separado só para documentar que "tudo nulo" é um caso
        // de uso válido e esperado (equivalente a "sem filtro"), não um erro.
        Sort ordenacaoEsperada = Sort.by(Sort.Direction.DESC, "dataHoraInicio");
        when(paradaRepository.findAll(any(Specification.class), eq(ordenacaoEsperada)))
                .thenReturn(List.of());

        List<Parada> resultado = paradaService.listarComFiltros(null, null, null, null, null);

        assertThat(resultado).isEmpty();
        verify(paradaRepository).findAll(any(Specification.class), eq(ordenacaoEsperada));
    }

    @Test
    void deveEditarParadaEGravarLogDeAuditoria() {
        // US09
        UUID paradaId = UUID.randomUUID();
        UUID novoMotivoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Instant inicioOriginal = Instant.parse("2026-07-20T10:00:00Z");
        Instant fimOriginal = Instant.parse("2026-07-20T10:30:00Z");

        Parada existente = Parada.builder()
                .id(paradaId)
                .dataHoraInicio(inicioOriginal)
                .dataHoraFim(fimOriginal)
                .duracaoMinutos(30L)
                .status(StatusParada.ENCERRADA)
                .build();

        MotivoParada novoMotivo = MotivoParada.builder().id(novoMotivoId).categoria("Elétrica").build();

        when(paradaRepository.buscarComRelacionamentosPorId(paradaId)).thenReturn(Optional.of(existente));
        when(motivoParadaRepository.findById(novoMotivoId)).thenReturn(Optional.of(novoMotivo));
        when(paradaRepository.save(any(Parada.class))).thenAnswer(inv -> inv.getArgument(0));

        EditarParadaRequest request = new EditarParadaRequest(novoMotivoId, "corrigido", inicioOriginal, null);

        Parada editada = paradaService.editarParada(paradaId, request, usuarioId);

        assertThat(editada.getMotivo().getId()).isEqualTo(novoMotivoId);
        assertThat(editada.getObservacoes()).isEqualTo("corrigido");
        // dataHoraFim não veio na requisição -> preserva o valor original, não zera.
        assertThat(editada.getDataHoraFim()).isEqualTo(fimOriginal);
        verify(auditoriaService).registrar("parada", paradaId, usuarioId, "UPDATE");
    }

    @Test
    void deveRecusarEditarComDataHoraFimAnteriorAoInicio() {
        // Parada já ENCERRADA sendo corrigida com um novo dataHoraInicio que
        // "ultrapassa" o dataHoraFim já registrado — RN01 continua valendo na edição.
        UUID paradaId = UUID.randomUUID();
        UUID motivoId = UUID.randomUUID();
        Instant inicioOriginal = Instant.parse("2026-07-20T10:00:00Z");
        Instant fimOriginal = inicioOriginal.plusSeconds(600);

        Parada encerrada = Parada.builder()
                .id(paradaId)
                .dataHoraInicio(inicioOriginal)
                .dataHoraFim(fimOriginal)
                .status(StatusParada.ENCERRADA)
                .build();

        when(paradaRepository.buscarComRelacionamentosPorId(paradaId)).thenReturn(Optional.of(encerrada));
        when(motivoParadaRepository.findById(motivoId))
                .thenReturn(Optional.of(MotivoParada.builder().id(motivoId).build()));

        Instant novoInicioInvalido = fimOriginal.plusSeconds(3600); // depois do fim já registrado
        EditarParadaRequest request = new EditarParadaRequest(motivoId, null, novoInicioInvalido, null);

        assertThatThrownBy(() -> paradaService.editarParada(paradaId, request, UUID.randomUUID()))
                .isInstanceOf(IntervaloInvalidoException.class);
    }

    @Test
    void deveRecusarDefinirDataHoraFimEmParadaAberta() {
        UUID paradaId = UUID.randomUUID();
        UUID motivoId = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-07-20T10:00:00Z");

        Parada aberta = Parada.builder()
                .id(paradaId)
                .dataHoraInicio(inicio)
                .status(StatusParada.ABERTA)
                .build();

        when(paradaRepository.buscarComRelacionamentosPorId(paradaId)).thenReturn(Optional.of(aberta));
        when(motivoParadaRepository.findById(motivoId))
                .thenReturn(Optional.of(MotivoParada.builder().id(motivoId).build()));

        EditarParadaRequest request = new EditarParadaRequest(motivoId, null, inicio, inicio.plusSeconds(600));

        assertThatThrownBy(() -> paradaService.editarParada(paradaId, request, UUID.randomUUID()))
                .isInstanceOf(IntervaloInvalidoException.class);
    }

    @Test
    void deveResolverSentinelasQuandoDatasSaoNulas() {
        // US10. dataInicio=null vira Instant.EPOCH, dataFim=null vira "agora" —
        // resolvido no Service, nunca chega null no repository (ver comentário
        // em ParadaRepository.buscarTempoTotalParadoPorEquipamento).
        List<TempoParadoPorEquipamento> esperado = List.of();
        when(paradaRepository.buscarTempoTotalParadoPorEquipamento(
                eq(StatusParada.ENCERRADA), eq(Instant.EPOCH), any(Instant.class)))
                .thenReturn(esperado);

        paradaService.tempoTotalParadoPorEquipamento(null, null);

        verify(paradaRepository).buscarTempoTotalParadoPorEquipamento(
                eq(StatusParada.ENCERRADA), eq(Instant.EPOCH), any(Instant.class));
    }

    @Test
    void deveRepassarDatasQuandoInformadas() {
        Instant dataInicio = Instant.parse("2026-07-01T00:00:00Z");
        Instant dataFim = Instant.parse("2026-07-31T23:59:59Z");
        List<TempoParadoPorEquipamento> esperado = List.of();

        when(paradaRepository.buscarTempoTotalParadoPorEquipamento(StatusParada.ENCERRADA, dataInicio, dataFim))
                .thenReturn(esperado);

        List<TempoParadoPorEquipamento> resultado =
                paradaService.tempoTotalParadoPorEquipamento(dataInicio, dataFim);

        assertThat(resultado).isEqualTo(esperado);
        verify(paradaRepository).buscarTempoTotalParadoPorEquipamento(StatusParada.ENCERRADA, dataInicio, dataFim);
    }
}
