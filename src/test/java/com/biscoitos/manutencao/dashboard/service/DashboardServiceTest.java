package com.biscoitos.manutencao.dashboard.service;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.dashboard.web.dto.DashboardOeeResponse;
import com.biscoitos.manutencao.paradas.service.ParadaService;
import com.biscoitos.manutencao.producao.repository.ApontamentoProducaoRepository;
import com.biscoitos.manutencao.producao.repository.TotaisProducao;
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
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EquipamentoRepository equipamentoRepository;
    @Mock
    private ParadaService paradaService;
    @Mock
    private ApontamentoProducaoRepository apontamentoProducaoRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private static TotaisProducao totais(long produzido, long boa) {
        return new TotaisProducao() {
            public Long getTotalProduzido() {
                return produzido;
            }

            public Long getTotalBoa() {
                return boa;
            }
        };
    }

    @Test
    void deveRecusarPeriodoInvalido() {
        Instant momento = Instant.now();
        assertThatThrownBy(() -> dashboardService.calcularOee(UUID.randomUUID(), momento, momento))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void deveFalharComEquipamentoInexistente() {
        UUID equipamentoId = UUID.randomUUID();
        when(equipamentoRepository.findById(equipamentoId)).thenReturn(Optional.empty());

        Instant inicio = Instant.now();
        assertThatThrownBy(() -> dashboardService.calcularOee(equipamentoId, inicio, inicio.plus(1, ChronoUnit.HOURS)))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void disponibilidadeSempreCalculavelMesmoSemProducaoNemCicloIdeal() {
        UUID equipamentoId = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        Instant fim = inicio.plus(8, ChronoUnit.HOURS); // 480 min

        when(equipamentoRepository.findById(equipamentoId))
                .thenReturn(Optional.of(Equipamento.builder().id(equipamentoId).nome("Forno").build()));
        when(paradaService.tempoParadoDoEquipamentoNoPeriodo(equipamentoId, inicio, fim)).thenReturn(60L);
        when(apontamentoProducaoRepository.somarProducaoDoEquipamentoNoPeriodo(equipamentoId, inicio, fim))
                .thenReturn(totais(0, 0));

        DashboardOeeResponse resposta = dashboardService.calcularOee(equipamentoId, inicio, fim);

        assertThat(resposta.tempoPlanejadoMinutos()).isEqualTo(480);
        assertThat(resposta.tempoParadoMinutos()).isEqualTo(60);
        assertThat(resposta.disponibilidade()).isCloseTo(420.0 / 480.0, offset(0.0001));
        assertThat(resposta.performance()).isNull();
        assertThat(resposta.qualidade()).isNull();
        assertThat(resposta.oee()).isNull();
    }

    @Test
    void performanceFicaNullSemCicloIdealConfiguradoMesmoComProducao() {
        UUID equipamentoId = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        Instant fim = inicio.plus(8, ChronoUnit.HOURS);

        Equipamento semCicloIdeal = Equipamento.builder().id(equipamentoId).nome("Forno").build(); // tempoCicloIdealSegundos = null

        when(equipamentoRepository.findById(equipamentoId)).thenReturn(Optional.of(semCicloIdeal));
        when(paradaService.tempoParadoDoEquipamentoNoPeriodo(equipamentoId, inicio, fim)).thenReturn(0L);
        when(apontamentoProducaoRepository.somarProducaoDoEquipamentoNoPeriodo(equipamentoId, inicio, fim))
                .thenReturn(totais(500, 480));

        DashboardOeeResponse resposta = dashboardService.calcularOee(equipamentoId, inicio, fim);

        assertThat(resposta.performance()).isNull();
        // Qualidade não depende de ciclo ideal — continua calculável.
        assertThat(resposta.qualidade()).isCloseTo(480.0 / 500.0, offset(0.0001));
        assertThat(resposta.oee()).isNull();
    }

    @Test
    void deveCalcularOeeCompletoQuandoTudoDisponivel() {
        UUID equipamentoId = UUID.randomUUID();
        Instant inicio = Instant.parse("2026-01-01T00:00:00Z");
        Instant fim = inicio.plus(8, ChronoUnit.HOURS); // 480 min planejado

        Equipamento comCicloIdeal = Equipamento.builder()
                .id(equipamentoId).nome("Forno").tempoCicloIdealSegundos(42).build();

        when(equipamentoRepository.findById(equipamentoId)).thenReturn(Optional.of(comCicloIdeal));
        when(paradaService.tempoParadoDoEquipamentoNoPeriodo(equipamentoId, inicio, fim)).thenReturn(60L);
        when(apontamentoProducaoRepository.somarProducaoDoEquipamentoNoPeriodo(equipamentoId, inicio, fim))
                .thenReturn(totais(500, 480));

        DashboardOeeResponse resposta = dashboardService.calcularOee(equipamentoId, inicio, fim);

        // tempoOperacao = 420 min = 25200s; performance = (42*500)/25200 = 5/6
        // disponibilidade = 420/480 = 7/8; qualidade = 480/500 = 24/25
        // OEE = 7/8 * 5/6 * 24/25 = 0.7 (conta exata, não é coincidência de arredondamento)
        assertThat(resposta.disponibilidade()).isCloseTo(0.875, offset(0.0001));
        assertThat(resposta.performance()).isCloseTo(0.8333, offset(0.0001));
        assertThat(resposta.qualidade()).isCloseTo(0.96, offset(0.0001));
        assertThat(resposta.oee()).isCloseTo(0.7, offset(0.0001));
    }
}
