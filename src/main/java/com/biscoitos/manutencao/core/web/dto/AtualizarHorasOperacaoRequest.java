package com.biscoitos.manutencao.core.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AtualizarHorasOperacaoRequest(
        @NotNull(message = "horas é obrigatório")
        @PositiveOrZero(message = "horas não pode ser negativo")
        Integer horas
) {
}
