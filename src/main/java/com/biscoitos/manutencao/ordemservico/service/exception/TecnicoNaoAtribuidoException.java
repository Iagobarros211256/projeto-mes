package com.biscoitos.manutencao.ordemservico.service.exception;

import java.util.UUID;

/** RN05 — não é possível iniciar execução sem um técnico responsável já atribuído. */
public class TecnicoNaoAtribuidoException extends RuntimeException {
    public TecnicoNaoAtribuidoException(UUID ordemServicoId) {
        super("A ordem de serviço " + ordemServicoId + " não tem técnico responsável atribuído");
    }
}
