package com.biscoitos.manutencao.estoque.web.dto;

import com.biscoitos.manutencao.estoque.domain.Peca;

import java.time.Instant;
import java.util.UUID;

public record PecaResponse(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        String unidadeMedida,
        Integer quantidadeAtual,
        Integer estoqueMinimo,
        Instant dataCriacao,
        boolean estoqueBaixo
) {
    public static PecaResponse from(Peca peca) {
        boolean estoqueBaixo = peca.getQuantidadeAtual() <= peca.getEstoqueMinimo();
        return new PecaResponse(
                peca.getId(),
                peca.getCodigo(),
                peca.getNome(),
                peca.getDescricao(),
                peca.getUnidadeMedida(),
                peca.getQuantidadeAtual(),
                peca.getEstoqueMinimo(),
                peca.getDataCriacao(),
                estoqueBaixo
        );
    }
}
