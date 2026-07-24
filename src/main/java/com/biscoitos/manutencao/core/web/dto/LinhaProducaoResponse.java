package com.biscoitos.manutencao.core.web.dto;

import com.biscoitos.manutencao.core.domain.LinhaProducao;

import java.util.UUID;

public record LinhaProducaoResponse(UUID id, String nome, String descricao) {
    public static LinhaProducaoResponse from(LinhaProducao linha) {
        return new LinhaProducaoResponse(linha.getId(), linha.getNome(), linha.getDescricao());
    }
}
