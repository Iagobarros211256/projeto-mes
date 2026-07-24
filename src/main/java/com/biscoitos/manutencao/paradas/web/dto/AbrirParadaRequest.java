package com.biscoitos.manutencao.paradas.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AbrirParadaRequest(
        @NotNull(message = "equipamentoId é obrigatório") UUID equipamentoId,
        @NotNull(message = "motivoId é obrigatório") UUID motivoId,
        String observacoes
) {
}
