package com.biscoitos.manutencao.paradas.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MotivoParadaRequest(
        @NotBlank(message = "categoria é obrigatória") String categoria,
        @NotBlank(message = "subcategoria é obrigatória") String subcategoria,
        String descricao
) {
}
