package com.biscoitos.manutencao.estoque.service.exception;

import java.util.UUID;

/** RN10 — uma saída não pode deixar quantidade_atual negativa. */
public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(UUID pecaId, int quantidadeAtual, int quantidadeSolicitada) {
        super("Peça " + pecaId + " tem " + quantidadeAtual
                + " em estoque; não é possível dar saída de " + quantidadeSolicitada);
    }
}
