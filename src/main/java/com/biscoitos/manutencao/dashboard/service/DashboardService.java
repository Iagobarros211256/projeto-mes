package com.biscoitos.manutencao.dashboard.service;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.dashboard.web.dto.DashboardOeeResponse;
import com.biscoitos.manutencao.paradas.repository.TempoParadoPorMotivo;
import com.biscoitos.manutencao.paradas.service.ParadaService;
import com.biscoitos.manutencao.producao.repository.ApontamentoProducaoRepository;
import com.biscoitos.manutencao.producao.repository.TotaisProducao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Dashboard não tem tabela própria — é uma camada de leitura sobre paradas, produção e
 * equipamento (ver modelo-dados-sprint6.md). Depende de paradas.service.ParadaService e
 * producao.repository.ApontamentoProducaoRepository, nunca o contrário: mesma direção
 * unidirecional já estabelecida entre os módulos de negócio desde o Sprint 3.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EquipamentoRepository equipamentoRepository;
    private final ParadaService paradaService;
    private final ApontamentoProducaoRepository apontamentoProducaoRepository;

    /**
     * Cada peça da fórmula documenta quando vira null em vez de um valor inventado —
     * ver a tabela completa de regras em backlog-sprint6.md.
     */
    public DashboardOeeResponse calcularOee(UUID equipamentoId, Instant dataInicio, Instant dataFim) {
        if (!dataFim.isAfter(dataInicio)) {
            throw new DadosInvalidosException(
                    "dataFim (" + dataFim + ") precisa ser depois de dataInicio (" + dataInicio + ")");
        }

        Equipamento equipamento = equipamentoRepository.findById(equipamentoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", equipamentoId));

        long tempoPlanejadoMinutos = Duration.between(dataInicio, dataFim).toMinutes();
        long tempoParadoMinutos = paradaService.tempoParadoDoEquipamentoNoPeriodo(equipamentoId, dataInicio, dataFim);
        long tempoOperacaoMinutos = tempoPlanejadoMinutos - tempoParadoMinutos;

        // Disponibilidade nunca é null: não depende de apontamento de produção.
        double disponibilidade = tempoPlanejadoMinutos > 0
                ? (double) tempoOperacaoMinutos / tempoPlanejadoMinutos
                : 0.0;

        TotaisProducao totais = apontamentoProducaoRepository.somarProducaoDoEquipamentoNoPeriodo(
                equipamentoId, dataInicio, dataFim);
        long quantidadeProduzida = totais.getTotalProduzido();
        long quantidadeBoa = totais.getTotalBoa();

        Integer tempoCicloIdealSegundos = equipamento.getTempoCicloIdealSegundos();

        // Performance — null se não há apontamento no período, ou se o equipamento não
        // tem ciclo ideal configurado (US28), ou se o tempo de operação é zero/negativo
        // (equipamento ficou parado o período inteiro planejado).
        Double performance = null;
        if (tempoCicloIdealSegundos != null && quantidadeProduzida > 0 && tempoOperacaoMinutos > 0) {
            double tempoOperacaoSegundos = tempoOperacaoMinutos * 60.0;
            performance = (tempoCicloIdealSegundos * quantidadeProduzida) / tempoOperacaoSegundos;
        }

        // Qualidade — null se não há apontamento no período (não existe "produziu zero,
        // então qualidade é 100%").
        Double qualidade = quantidadeProduzida > 0 ? (double) quantidadeBoa / quantidadeProduzida : null;

        Double oee = (performance != null && qualidade != null)
                ? disponibilidade * performance * qualidade
                : null;

        return new DashboardOeeResponse(
                equipamento.getId(),
                equipamento.getNome(),
                dataInicio,
                dataFim,
                tempoPlanejadoMinutos,
                tempoParadoMinutos,
                tempoOperacaoMinutos,
                disponibilidade,
                quantidadeProduzida,
                quantidadeBoa,
                performance,
                qualidade,
                oee
        );
    }

    public List<TempoParadoPorMotivo> principaisCausasParada(UUID equipamentoId, Instant dataInicio, Instant dataFim) {
        return paradaService.principaisCausasParada(equipamentoId, dataInicio, dataFim);
    }
}
