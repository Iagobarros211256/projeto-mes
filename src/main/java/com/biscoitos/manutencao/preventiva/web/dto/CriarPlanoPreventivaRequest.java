package com.biscoitos.manutencao.preventiva.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * intervaloDias e intervaloHoras são ambos opcionais no DTO — o Service valida que pelo
 * menos um veio preenchido (RN08). Mesma abordagem do AbrirOrdemServicoRequest (US11).
 */
public record CriarPlanoPreventivaRequest(
        @NotNull(message = "equipamentoId é obrigatório") UUID equipamentoId,
        @NotBlank(message = "titulo é obrigatório") String titulo,
        String descricao,
        @Positive(message = "intervaloDias deve ser positivo") Integer intervaloDias,
        @Positive(message = "intervaloHoras deve ser positivo") Integer intervaloHoras
) {
}
