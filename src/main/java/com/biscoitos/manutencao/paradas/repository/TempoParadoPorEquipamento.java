package com.biscoitos.manutencao.paradas.repository;

import java.util.UUID;

/**
 * Projeção do Spring Data — os nomes dos getters precisam bater com os aliases (AS)
 * da query em ParadaRepository.buscarTempoTotalParadoPorEquipamento.
 */
public interface TempoParadoPorEquipamento {
    UUID getEquipamentoId();
    String getEquipamentoNome();
    Long getTempoTotalMinutos();
}
