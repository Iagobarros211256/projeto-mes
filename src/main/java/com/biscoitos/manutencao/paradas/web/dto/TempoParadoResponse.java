package com.biscoitos.manutencao.paradas.web.dto;

import com.biscoitos.manutencao.paradas.repository.TempoParadoPorEquipamento;

import java.util.UUID;

public record TempoParadoResponse(UUID equipamentoId, String equipamentoNome, Long tempoTotalMinutos) {
    public static TempoParadoResponse from(TempoParadoPorEquipamento projecao) {
        return new TempoParadoResponse(
                projecao.getEquipamentoId(),
                projecao.getEquipamentoNome(),
                projecao.getTempoTotalMinutos()
        );
    }
}
