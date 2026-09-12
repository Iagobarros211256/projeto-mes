package com.biscoitos.manutencao.ordemservico.service.exception;

import com.biscoitos.manutencao.ordemservico.domain.StatusOrdemServico;

import java.util.UUID;

/** RN07 — uma OS CONCLUIDA ou CANCELADA não aceita mais nenhuma transição de status. */
public class OrdemServicoEmEstadoTerminalException extends RuntimeException {
    public OrdemServicoEmEstadoTerminalException(UUID ordemServicoId, StatusOrdemServico status) {
        super("A ordem de serviço " + ordemServicoId + " já está em estado terminal (" + status + ")");
    }
}
