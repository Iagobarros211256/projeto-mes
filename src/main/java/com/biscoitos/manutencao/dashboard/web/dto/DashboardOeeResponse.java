package com.biscoitos.manutencao.dashboard.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * performance, qualidade e oee vêm null quando não dá pra calcular de verdade — ver
 * regras em backlog-sprint6.md. disponibilidade nunca é null (não depende de apontamento
 * de produção, só de paradas).
 */
public record DashboardOeeResponse(
        UUID equipamentoId,
        String equipamentoNome,
        Instant periodoInicio,
        Instant periodoFim,
        long tempoPlanejadoMinutos,
        long tempoParadoMinutos,
        long tempoOperacaoMinutos,
        double disponibilidade,
        long quantidadeProduzida,
        long quantidadeBoa,
        Double performance,
        Double qualidade,
        Double oee
) {
}
