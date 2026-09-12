package com.biscoitos.manutencao.core.web.dto;

import com.biscoitos.manutencao.core.domain.Equipamento;

import java.util.UUID;

public record EquipamentoResponse(
        UUID id,
        String codigo,
        String nome,
        String setor,
        String status,
        UUID linhaProducaoId,
        String linhaProducaoNome,
        Integer horasOperacaoAcumuladas,
        Integer tempoCicloIdealSegundos
) {
    public static EquipamentoResponse from(Equipamento equipamento) {
        return new EquipamentoResponse(
                equipamento.getId(),
                equipamento.getCodigo(),
                equipamento.getNome(),
                equipamento.getSetor(),
                equipamento.getStatus().name(),
                equipamento.getLinhaProducao().getId(),
                equipamento.getLinhaProducao().getNome(),
                equipamento.getHorasOperacaoAcumuladas(),
                equipamento.getTempoCicloIdealSegundos()
        );
    }
}
