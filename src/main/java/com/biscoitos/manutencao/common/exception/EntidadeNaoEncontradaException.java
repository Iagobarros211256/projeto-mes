package com.biscoitos.manutencao.common.exception;

public class EntidadeNaoEncontradaException extends RuntimeException {
    public EntidadeNaoEncontradaException(String entidade, Object id) {
        super(entidade + " não encontrado(a): " + id);
    }
}
