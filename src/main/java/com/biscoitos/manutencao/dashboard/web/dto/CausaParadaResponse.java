package com.biscoitos.manutencao.dashboard.web.dto;

import com.biscoitos.manutencao.paradas.repository.TempoParadoPorMotivo;

import java.util.UUID;

public record CausaParadaResponse(UUID motivoId, String categoria, String subcategoria, Long tempoTotalMinutos) {
    public static CausaParadaResponse from(TempoParadoPorMotivo projecao) {
        return new CausaParadaResponse(
                projecao.getMotivoId(),
                projecao.getCategoria(),
                projecao.getSubcategoria(),
                projecao.getTempoTotalMinutos()
        );
    }
}
