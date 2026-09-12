package com.biscoitos.manutencao.common.exception;

import java.util.UUID;

/** RN09 — horas_operacao_acumuladas só aceita valores maiores ou iguais ao atual. */
public class HorometroRetrocessoException extends RuntimeException {
    public HorometroRetrocessoException(UUID equipamentoId, int horasAtuais, int horasInformadas) {
        super("O equipamento " + equipamentoId + " já tem " + horasAtuais
                + "h registradas; não é possível informar " + horasInformadas + "h (retrocesso)");
    }
}
