package com.biscoitos.manutencao.paradas.service.exception;

/** RN01 — a parada não pode ser encerrada com hora de fim anterior à hora de início. */
public class IntervaloInvalidoException extends RuntimeException {
    public IntervaloInvalidoException(String message) {
        super(message);
    }
}
