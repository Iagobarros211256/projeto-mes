package com.biscoitos.manutencao.paradas.web.dto;

import com.biscoitos.manutencao.paradas.domain.MotivoParada;

import java.util.UUID;

public record MotivoParadaResponse(UUID id, String categoria, String subcategoria, String descricao) {
    public static MotivoParadaResponse from(MotivoParada motivo) {
        return new MotivoParadaResponse(motivo.getId(), motivo.getCategoria(), motivo.getSubcategoria(), motivo.getDescricao());
    }
}
