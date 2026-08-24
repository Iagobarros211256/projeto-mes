package com.biscoitos.manutencao.ordemservico.web.dto;

import com.biscoitos.manutencao.ordemservico.domain.OrdemServico;

import java.time.Instant;
import java.util.UUID;

public record OrdemServicoResponse(
        UUID id,
        UUID equipamentoId,
        String equipamentoNome,
        String titulo,
        String descricao,
        String tipo,
        String prioridade,
        String status,
        UUID solicitanteId,
        String solicitanteNome,
        UUID tecnicoResponsavelId,
        String tecnicoResponsavelNome,
        Instant dataAbertura,
        Instant dataInicioExecucao,
        Instant dataConclusao,
        String observacoesEncerramento
) {
    public static OrdemServicoResponse from(OrdemServico os) {
        boolean temTecnico = os.getTecnicoResponsavel() != null;
        return new OrdemServicoResponse(
                os.getId(),
                os.getEquipamento().getId(),
                os.getEquipamento().getNome(),
                os.getTitulo(),
                os.getDescricao(),
                os.getTipo().name(),
                os.getPrioridade().name(),
                os.getStatus().name(),
                os.getSolicitante().getId(),
                os.getSolicitante().getNome(),
                temTecnico ? os.getTecnicoResponsavel().getId() : null,
                temTecnico ? os.getTecnicoResponsavel().getNome() : null,
                os.getDataAbertura(),
                os.getDataInicioExecucao(),
                os.getDataConclusao(),
                os.getObservacoesEncerramento()
        );
    }
}
