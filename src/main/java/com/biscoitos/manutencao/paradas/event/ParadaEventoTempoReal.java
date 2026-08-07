package com.biscoitos.manutencao.paradas.event;

import com.biscoitos.manutencao.paradas.web.dto.ParadaResponse;

/**
 * O payload é o ParadaResponse já montado (não a entidade Parada) de propósito: o
 * ParadaService monta esse DTO ainda DENTRO da transação, com equipamento/motivo/
 * responsavel já carregados via JOIN FETCH. Se publicássemos a entidade e deixássemos
 * o listener acessar esses relacionamentos depois (@TransactionalEventListener roda
 * após o commit, sessão já fechada), seria o mesmo LazyInitializationException que já
 * corrigimos duas vezes no Sprint 1/2 — aqui a gente evita o problema de origem.
 */
public record ParadaEventoTempoReal(ParadaResponse parada, String tipoEvento) {

    public static ParadaEventoTempoReal aberta(ParadaResponse parada) {
        return new ParadaEventoTempoReal(parada, "ABERTA");
    }

    public static ParadaEventoTempoReal encerrada(ParadaResponse parada) {
        return new ParadaEventoTempoReal(parada, "ENCERRADA");
    }
}
