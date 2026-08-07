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
import com.biscoitos.manutencao.paradas.service.exception.IntervaloInvalidoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaEmAbertoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaJaEncerradaException;
import com.biscoitos.manutencao.paradas.web.dto.AbrirParadaRequest;
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
                .build();

        when(paradaRepository.buscarComRelacionamentosPorId(paradaId)).thenReturn(Optional.of(parada));
        when(paradaRepository.save(any(Parada.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Parada encerrada = paradaService.encerrarParada(paradaId, new EncerrarParadaRequest(fim));

        assertThat(encerrada.getStatus()).isEqualTo(StatusParada.ENCERRADA);
        assertThat(encerrada.getDuracaoMinutos()).isEqualTo(45L);
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
}
