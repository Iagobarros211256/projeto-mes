package com.biscoitos.manutencao.paradas.service.exception;

import java.util.UUID;

public class ParadaJaEncerradaException extends RuntimeException {
    public ParadaJaEncerradaException(UUID paradaId) {
        super("A parada " + paradaId + " já está encerrada");
    }
}
