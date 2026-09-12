package com.biscoitos.manutencao.paradas.repository;

import java.util.UUID;

/** Mesma ideia de TempoParadoPorEquipamento (US10), agrupado por motivo em vez de equipamento. */
public interface TempoParadoPorMotivo {
    UUID getMotivoId();
    String getCategoria();
    String getSubcategoria();
    Long getTempoTotalMinutos();
}
