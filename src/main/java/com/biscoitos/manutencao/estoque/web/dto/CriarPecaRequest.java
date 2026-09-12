package com.biscoitos.manutencao.estoque.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CriarPecaRequest(
        @NotBlank(message = "codigo é obrigatório") String codigo,
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao,
        @NotBlank(message = "unidadeMedida é obrigatória") String unidadeMedida,
        @NotNull(message = "estoqueMinimo é obrigatório")
        @PositiveOrZero(message = "estoqueMinimo não pode ser negativo")
        Integer estoqueMinimo
) {
}
