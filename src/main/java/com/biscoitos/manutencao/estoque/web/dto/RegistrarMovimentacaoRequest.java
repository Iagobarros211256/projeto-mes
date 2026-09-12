package com.biscoitos.manutencao.estoque.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/** RN11 — quantidade sempre positiva; o sinal da operação vem do endpoint chamado, não do valor. */
public record RegistrarMovimentacaoRequest(
        @NotNull(message = "quantidade é obrigatória")
        @Positive(message = "quantidade deve ser positiva")
        Integer quantidade,
        String observacao,
        UUID ordemServicoId
) {
}
