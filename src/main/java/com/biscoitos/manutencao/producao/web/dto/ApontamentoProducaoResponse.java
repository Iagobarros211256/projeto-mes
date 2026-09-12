package com.biscoitos.manutencao.producao.web.dto;

import com.biscoitos.manutencao.producao.domain.ApontamentoProducao;

import java.time.Instant;
import java.util.UUID;

public record ApontamentoProducaoResponse(
        UUID id,
        UUID equipamentoId,
        String equipamentoNome,
        Instant dataHoraInicio,
        Instant dataHoraFim,
        Integer quantidadeProduzida,
        Integer quantidadeBoa,
        UUID responsavelId,
        String responsavelNome,
        Instant dataRegistro
) {
    public static ApontamentoProducaoResponse from(ApontamentoProducao a) {
        return new ApontamentoProducaoResponse(
                a.getId(),
                a.getEquipamento().getId(),
                a.getEquipamento().getNome(),
                a.getDataHoraInicio(),
                a.getDataHoraFim(),
                a.getQuantidadeProduzida(),
                a.getQuantidadeBoa(),
                a.getResponsavel().getId(),
                a.getResponsavel().getNome(),
                a.getDataRegistro()
        );
    }
}
