package com.biscoitos.manutencao.paradas.web.dto;

import com.biscoitos.manutencao.paradas.domain.Parada;

import java.time.Instant;
import java.util.UUID;

public record ParadaResponse(
        UUID id,
        UUID equipamentoId,
        String equipamentoNome,
        UUID motivoId,
        String motivoCategoria,
        String motivoSubcategoria,
        UUID responsavelId,
        String responsavelNome,
        Instant dataHoraInicio,
        Instant dataHoraFim,
        Long duracaoMinutos,
        String observacoes,
        String status
) {
    public static ParadaResponse from(Parada parada) {
        return new ParadaResponse(
                parada.getId(),
                parada.getEquipamento().getId(),
                parada.getEquipamento().getNome(),
                parada.getMotivo().getId(),
                parada.getMotivo().getCategoria(),
                parada.getMotivo().getSubcategoria(),
                parada.getResponsavel().getId(),
                parada.getResponsavel().getNome(),
                parada.getDataHoraInicio(),
                parada.getDataHoraFim(),
                parada.getDuracaoMinutos(),
                parada.getObservacoes(),
                parada.getStatus().name()
        );
    }
}
