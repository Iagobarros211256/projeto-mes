package com.biscoitos.manutencao.paradas.repository;

import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.domain.StatusParada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParadaRepository extends JpaRepository<Parada, UUID> {

    /**
     * Sustenta a RN02: um equipamento não pode ter duas paradas ABERTA ao mesmo tempo.
     * Existe um índice (equipamento_id, status) na migration V2 para essa consulta não
     * degradar conforme o histórico de paradas cresce.
     */
    boolean existsByEquipamentoIdAndStatus(UUID equipamentoId, StatusParada status);

    /**
     * equipamento, motivo e responsavel são LAZY. Com open-in-view=false, o ParadaResponse
     * (montado no Controller) é lido fora da sessão do Hibernate — sem JOIN FETCH aqui,
     * qualquer um desses três campos dispara LazyInitializationException na hora de montar
     * o DTO. Bug real encontrado em teste manual no Sprint 1 (não pego pelos testes com
     * Mockito, porque mock não simula sessão/proxy do Hibernate).
     */
    @Query("SELECT p FROM Parada p JOIN FETCH p.equipamento JOIN FETCH p.motivo JOIN FETCH p.responsavel ORDER BY p.dataHoraInicio DESC")
    List<Parada> findAllComRelacionamentos();

    @Query("SELECT p FROM Parada p JOIN FETCH p.equipamento JOIN FETCH p.motivo JOIN FETCH p.responsavel WHERE p.id = :id")
    Optional<Parada> buscarComRelacionamentosPorId(UUID id);
}
