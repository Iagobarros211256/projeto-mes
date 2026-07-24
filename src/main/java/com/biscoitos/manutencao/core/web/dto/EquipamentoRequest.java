package com.biscoitos.manutencao.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EquipamentoRequest(
        @NotBlank(message = "código é obrigatório") String codigo,
        @NotBlank(message = "nome é obrigatório") String nome,
        String setor,
        @NotNull(message = "linhaProducaoId é obrigatório") UUID linhaProducaoId
) {
}
