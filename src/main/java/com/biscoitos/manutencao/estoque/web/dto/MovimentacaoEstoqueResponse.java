package com.biscoitos.manutencao.estoque.web.dto;

import com.biscoitos.manutencao.estoque.domain.MovimentacaoEstoque;

import java.time.Instant;
import java.util.UUID;

public record MovimentacaoEstoqueResponse(
        UUID id,
        UUID pecaId,
        String pecaNome,
        String tipo,
        Integer quantidade,
        String observacao,
        UUID responsavelId,
        String responsavelNome,
        Instant dataHora,
        UUID ordemServicoId
) {
    public static MovimentacaoEstoqueResponse from(MovimentacaoEstoque mov) {
        return new MovimentacaoEstoqueResponse(
                mov.getId(),
                mov.getPeca().getId(),
                mov.getPeca().getNome(),
                mov.getTipo().name(),
                mov.getQuantidade(),
                mov.getObservacao(),
                mov.getResponsavel().getId(),
                mov.getResponsavel().getNome(),
                mov.getDataHora(),
                mov.getOrdemServicoId()
        );
    }
}
