package com.biscoitos.manutencao.preventiva.web.dto;

import com.biscoitos.manutencao.preventiva.domain.PlanoPreventiva;

import java.time.Instant;
import java.util.UUID;

public record PlanoPreventivaResponse(
        UUID id,
        UUID equipamentoId,
        String equipamentoNome,
        String titulo,
        String descricao,
        Integer intervaloDias,
        Integer intervaloHoras,
        Instant dataUltimaExecucao,
        Integer horasNaUltimaExecucao,
        UUID ultimaOrdemServicoId,
        boolean ativo,
        Instant dataCriacao,
        boolean vencido
) {
    public static PlanoPreventivaResponse from(PlanoPreventiva plano, boolean vencido) {
        return new PlanoPreventivaResponse(
                plano.getId(),
                plano.getEquipamento().getId(),
                plano.getEquipamento().getNome(),
                plano.getTitulo(),
                plano.getDescricao(),
                plano.getIntervaloDias(),
                plano.getIntervaloHoras(),
                plano.getDataUltimaExecucao(),
                plano.getHorasNaUltimaExecucao(),
                plano.getUltimaOrdemServicoId(),
                plano.isAtivo(),
                plano.getDataCriacao(),
                vencido
        );
    }
}
