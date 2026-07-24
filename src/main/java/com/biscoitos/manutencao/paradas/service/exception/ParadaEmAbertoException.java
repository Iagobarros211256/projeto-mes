package com.biscoitos.manutencao.paradas.service.exception;

import java.util.UUID;

/** RN02 — um equipamento não pode ter duas paradas em aberto simultaneamente. */
public class ParadaEmAbertoException extends RuntimeException {
    public ParadaEmAbertoException(UUID equipamentoId) {
        super("O equipamento " + equipamentoId + " já possui uma parada em aberto");
    }
}
