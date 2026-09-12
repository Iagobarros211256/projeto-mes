package com.biscoitos.manutencao.producao.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

public record RegistrarApontamentoRequest(
        @NotNull(message = "equipamentoId é obrigatório") UUID equipamentoId,
        @NotNull(message = "dataHoraInicio é obrigatório") Instant dataHoraInicio,
        @NotNull(message = "dataHoraFim é obrigatório") Instant dataHoraFim,
        @NotNull(message = "quantidadeProduzida é obrigatória")
        @PositiveOrZero(message = "quantidadeProduzida não pode ser negativa")
        Integer quantidadeProduzida,
        @NotNull(message = "quantidadeBoa é obrigatória")
        @PositiveOrZero(message = "quantidadeBoa não pode ser negativa")
        Integer quantidadeBoa
) {
}
