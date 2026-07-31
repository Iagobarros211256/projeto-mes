package com.biscoitos.manutencao.paradas.repository;

import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.domain.StatusParada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * JpaSpecificationExecutor sustenta o findAll(Specification, Sort) usado pela US08
 * (ver ParadaSpecifications). Duas tentativas de resolver os filtros opcionais via JPQL
 * puro ("(:param IS NULL OR campo = :param)", com e sem CAST explícito) esbarraram em
 * bugs reais de inferência de tipo do driver do Postgres — Specification evita a classe
 * inteira desse problema porque o predicado só é adicionado quando o filtro não é nulo.
 */
public interface ParadaRepository extends JpaRepository<Parada, UUID>, JpaSpecificationExecutor<Parada> {

    /**
     * Sustenta a RN02: um equipamento não pode ter duas paradas ABERTA ao mesmo tempo.
     * Existe um índice (equipamento_id, status) na migration V2 para essa consulta não
     * degradar conforme o histórico de paradas cresce.
     */
    boolean existsByEquipamentoIdAndStatus(UUID equipamentoId, StatusParada status);

    @Query("SELECT p FROM Parada p JOIN FETCH p.equipamento JOIN FETCH p.motivo JOIN FETCH p.responsavel WHERE p.id = :id")
    Optional<Parada> buscarComRelacionamentosPorId(UUID id);
}
