package com.biscoitos.manutencao.core.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AtualizarTempoCicloIdealRequest(
        @NotNull(message = "segundos é obrigatório")
        @Positive(message = "segundos deve ser positivo")
        Integer segundos
) {
}
