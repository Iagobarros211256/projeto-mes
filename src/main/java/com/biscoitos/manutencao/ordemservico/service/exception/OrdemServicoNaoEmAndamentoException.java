package com.biscoitos.manutencao.ordemservico.service.exception;

import java.util.UUID;

/** RN06 — só é possível concluir uma OS que esteja EM_ANDAMENTO. */
public class OrdemServicoNaoEmAndamentoException extends RuntimeException {
    public OrdemServicoNaoEmAndamentoException(UUID ordemServicoId) {
        super("A ordem de serviço " + ordemServicoId + " não está EM_ANDAMENTO");
    }
}
