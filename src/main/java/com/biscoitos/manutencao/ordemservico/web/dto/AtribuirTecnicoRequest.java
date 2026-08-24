package com.biscoitos.manutencao.ordemservico.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AtribuirTecnicoRequest(
        @NotNull(message = "tecnicoId é obrigatório") UUID tecnicoId
) {
}
