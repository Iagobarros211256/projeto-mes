package com.biscoitos.manutencao.core.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LinhaProducaoRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao
) {
}
