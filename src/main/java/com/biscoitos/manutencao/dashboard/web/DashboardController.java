package com.biscoitos.manutencao.dashboard.web;

import com.biscoitos.manutencao.dashboard.service.DashboardService;
import com.biscoitos.manutencao.dashboard.web.dto.CausaParadaResponse;
import com.biscoitos.manutencao.dashboard.web.dto.DashboardOeeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Todos os GETs — qualquer autenticado (é dashboard de leitura, não muda estado de nada). */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * US29. equipamentoId, dataInicio e dataFim são OBRIGATÓRIOS aqui — diferente de
     * outros filtros do sistema, "tempo planejado" não existe sem um período explícito.
     */
    @GetMapping("/oee")
    public DashboardOeeResponse oee(
            @RequestParam UUID equipamentoId,
            @RequestParam Instant dataInicio,
            @RequestParam Instant dataFim
    ) {
        return dashboardService.calcularOee(equipamentoId, dataInicio, dataFim);
    }

    /** US30 — equipamentoId opcional (ranking geral ou de um equipamento só); período opcional (default = todo o histórico até agora). */
    @GetMapping("/principais-causas-parada")
    public List<CausaParadaResponse> principaisCausasParada(
            @RequestParam(required = false) UUID equipamentoId,
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim
    ) {
        return dashboardService.principaisCausasParada(equipamentoId, dataInicio, dataFim).stream()
                .map(CausaParadaResponse::from)
                .toList();
    }
}
