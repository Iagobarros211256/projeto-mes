package com.biscoitos.manutencao.preventiva.service;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.ordemservico.repository.OrdemServicoRepository;
import com.biscoitos.manutencao.preventiva.domain.PlanoPreventiva;
import com.biscoitos.manutencao.preventiva.repository.PlanoPreventivaRepository;
import com.biscoitos.manutencao.preventiva.web.dto.CriarPlanoPreventivaRequest;
import com.biscoitos.manutencao.preventiva.web.dto.RegistrarExecucaoRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanoPreventivaServiceTest {

    @Mock
    private PlanoPreventivaRepository planoPreventivaRepository;
    @Mock
    private EquipamentoRepository equipamentoRepository;
    @Mock
    private OrdemServicoRepository ordemServicoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PlanoPreventivaService planoPreventivaService;

    @Test
    void deveRecusarCriarSemNenhumIntervalo() {
        // RN08
        CriarPlanoPreventivaRequest request = new CriarPlanoPreventivaRequest(
                UUID.randomUUID(), "Troca de óleo", null, null, null);

        assertThatThrownBy(() -> planoPreventivaService.criar(request, UUID.randomUUID()))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void deveCriarComApenasIntervaloDias() {
        UUID equipamentoId = UUID.randomUUID();
        when(equipamentoRepository.findById(equipamentoId))
                .thenReturn(Optional.of(Equipamento.builder().id(equipamentoId).build()));
        when(planoPreventivaRepository.save(any(PlanoPreventiva.class))).thenAnswer(inv -> inv.getArgument(0));

        CriarPlanoPreventivaRequest request = new CriarPlanoPreventivaRequest(
                equipamentoId, "Limpeza", null, 30, null);

        PlanoPreventiva plano = planoPreventivaService.criar(request, UUID.randomUUID());

        assertThat(plano.getIntervaloDias()).isEqualTo(30);
        assertThat(plano.getIntervaloHoras()).isNull();
        assertThat(plano.isAtivo()).isTrue();
    }

    @Test
    void planoNuncaExecutadoEstaSempreVencido() {
        PlanoPreventiva plano = PlanoPreventiva.builder()
                .id(UUID.randomUUID())
                .equipamento(Equipamento.builder().horasOperacaoAcumuladas(0).build())
                .intervaloDias(30)
                .dataUltimaExecucao(null)
                .build();

        assertThat(planoPreventivaService.estaVencido(plano)).isTrue();
    }

    @Test
    void planoPorDiasNaoVencidoDentroDoIntervalo() {
        PlanoPreventiva plano = PlanoPreventiva.builder()
                .id(UUID.randomUUID())
                .equipamento(Equipamento.builder().horasOperacaoAcumuladas(0).build())
                .intervaloDias(30)
                .dataUltimaExecucao(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        assertThat(planoPreventivaService.estaVencido(plano)).isFalse();
    }

    @Test
    void planoPorDiasVencidoQuandoPassaDoIntervalo() {
        PlanoPreventiva plano = PlanoPreventiva.builder()
                .id(UUID.randomUUID())
                .equipamento(Equipamento.builder().horasOperacaoAcumuladas(0).build())
                .intervaloDias(30)
                .dataUltimaExecucao(Instant.now().minus(31, ChronoUnit.DAYS))
                .build();

        assertThat(planoPreventivaService.estaVencido(plano)).isTrue();
    }

    @Test
    void planoPorHorasNaoVencidoDentroDoIntervalo() {
        PlanoPreventiva plano = PlanoPreventiva.builder()
                .id(UUID.randomUUID())
                .equipamento(Equipamento.builder().horasOperacaoAcumuladas(450).build())
                .intervaloHoras(500)
                .horasNaUltimaExecucao(0)
                .dataUltimaExecucao(Instant.now()) // presente pra não cair no "nunca executado"
                .build();

        assertThat(planoPreventivaService.estaVencido(plano)).isFalse();
    }

    @Test
    void planoPorHorasVencidoQuandoPassaDoIntervalo() {
        PlanoPreventiva plano = PlanoPreventiva.builder()
                .id(UUID.randomUUID())
                .equipamento(Equipamento.builder().horasOperacaoAcumuladas(550).build())
                .intervaloHoras(500)
                .horasNaUltimaExecucao(0)
                .dataUltimaExecucao(Instant.now())
                .build();

        assertThat(planoPreventivaService.estaVencido(plano)).isTrue();
    }

    @Test
    void planoComAmbosCriteriosVenceSeQualquerUmVencer() {
        // Por dias ainda não venceu, mas por horas já passou — precisa vencer.
        PlanoPreventiva plano = PlanoPreventiva.builder()
                .id(UUID.randomUUID())
                .equipamento(Equipamento.builder().horasOperacaoAcumuladas(600).build())
                .intervaloDias(90)
                .intervaloHoras(500)
                .dataUltimaExecucao(Instant.now().minus(5, ChronoUnit.DAYS))
                .horasNaUltimaExecucao(0)
                .build();

        assertThat(planoPreventivaService.estaVencido(plano)).isTrue();
    }

    @Test
    void deveRegistrarExecucaoEZerarContadores() {
        UUID planoId = UUID.randomUUID();
        Equipamento equipamento = Equipamento.builder().horasOperacaoAcumuladas(700).build();
        PlanoPreventiva plano = PlanoPreventiva.builder()
                .id(planoId)
                .equipamento(equipamento)
                .intervaloHoras(500)
                .horasNaUltimaExecucao(0)
                .dataUltimaExecucao(Instant.now().minus(100, ChronoUnit.DAYS))
                .build();

        when(planoPreventivaRepository.buscarComEquipamentoPorId(planoId)).thenReturn(Optional.of(plano));
        when(planoPreventivaRepository.save(any(PlanoPreventiva.class))).thenAnswer(inv -> inv.getArgument(0));

        PlanoPreventiva executado = planoPreventivaService.registrarExecucao(
                planoId, new RegistrarExecucaoRequest(null), UUID.randomUUID());

        assertThat(executado.getHorasNaUltimaExecucao()).isEqualTo(700);
        assertThat(planoPreventivaService.estaVencido(executado)).isFalse();
    }

    @Test
    void deveFalharAoRegistrarExecucaoComOrdemServicoInexistente() {
        UUID planoId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        PlanoPreventiva plano = PlanoPreventiva.builder()
                .id(planoId)
                .equipamento(Equipamento.builder().horasOperacaoAcumuladas(0).build())
                .intervaloDias(30)
                .build();

        when(planoPreventivaRepository.buscarComEquipamentoPorId(planoId)).thenReturn(Optional.of(plano));
        when(ordemServicoRepository.existsById(osId)).thenReturn(false);

        assertThatThrownBy(() -> planoPreventivaService.registrarExecucao(
                planoId, new RegistrarExecucaoRequest(osId), UUID.randomUUID()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }
}
