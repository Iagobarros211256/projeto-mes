package com.biscoitos.manutencao.paradas.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Equipamento não é editável aqui de propósito: trocar o equipamento de uma parada já
 * registrada muda o "fato" que aconteceu, não corrige um erro de digitação — se o
 * equipamento errado foi selecionado, o caminho correto é reportar e tratar como um caso
 * à parte (fora do escopo desta story).
 *
 * dataHoraFim só é aceito se a parada já estiver ENCERRADA (validado no Service);
 * numa parada ABERTA, o caminho pra registrar o fim é o endpoint de encerrar, não este.
 */
public record EditarParadaRequest(
        @NotNull(message = "motivoId é obrigatório") UUID motivoId,
        String observacoes,
        @NotNull(message = "dataHoraInicio é obrigatório") Instant dataHoraInicio,
        Instant dataHoraFim
) {
}
