package com.biscoitos.manutencao.paradas.repository;

import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.domain.StatusParada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
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

    /**
     * US10 (RF07). dataInicio/dataFim NUNCA chegam nulas aqui — o Service (ver
     * ParadaService.tempoTotalParadoPorEquipamento) já resolve valores-sentinela antes de
     * chamar este método. Isso é deliberado: a US08 quebrou duas vezes tentando aceitar
     * parâmetros de data opcionalmente nulos direto numa query JPQL contra o Postgres.
     * Aqui é mais simples ainda evitar o problema inteiro, já que essa agregação não tem
     * outros filtros opcionais que justifiquem Specification.
     *
     * Só considera paradas ENCERRADA — uma parada ainda ABERTA não tem duracaoMinutos
     * final, então somar sua duração parcial seria um número que muda a cada segundo
     * (isso é assunto pro futuro Dashboard de OEE em tempo real, Fase 3, não pra este
     * relatório).
     */
    @Query("""
            SELECT p.equipamento.id AS equipamentoId,
                   p.equipamento.nome AS equipamentoNome,
                   COALESCE(SUM(p.duracaoMinutos), 0) AS tempoTotalMinutos
            FROM Parada p
            WHERE p.status = :status
              AND p.dataHoraInicio >= :dataInicio
              AND p.dataHoraInicio <= :dataFim
            GROUP BY p.equipamento.id, p.equipamento.nome
            ORDER BY COALESCE(SUM(p.duracaoMinutos), 0) DESC
            """)
    List<TempoParadoPorEquipamento> buscarTempoTotalParadoPorEquipamento(
            @Param("status") StatusParada status,
            @Param("dataInicio") Instant dataInicio,
            @Param("dataFim") Instant dataFim
    );

    /**
     * US29 (Dashboard de OEE) — soma escalar pra UM equipamento num período exato. Não
     * usa sentinelas como a US10: o endpoint de OEE exige dataInicio/dataFim explícitos
     * (é o próprio "tempo planejado" da fórmula), então esses parâmetros já chegam
     * concretos por definição, nunca nulos.
     */
    @Query("""
            SELECT COALESCE(SUM(p.duracaoMinutos), 0)
            FROM Parada p
            WHERE p.status = :status
              AND p.equipamento.id = :equipamentoId
              AND p.dataHoraInicio >= :dataInicio
              AND p.dataHoraInicio <= :dataFim
            """)
    Long somarTempoParadoDoEquipamentoNoPeriodo(
            @Param("status") StatusParada status,
            @Param("equipamentoId") UUID equipamentoId,
            @Param("dataInicio") Instant dataInicio,
            @Param("dataFim") Instant dataFim
    );

    /**
     * US30 — ranking de causas de parada, todos os equipamentos. dataInicio/dataFim
     * resolvidos com sentinela no Service (mesmo padrão da US10), nunca nulos aqui.
     */
    @Query("""
            SELECT p.motivo.id AS motivoId,
                   p.motivo.categoria AS categoria,
                   p.motivo.subcategoria AS subcategoria,
                   COALESCE(SUM(p.duracaoMinutos), 0) AS tempoTotalMinutos
            FROM Parada p
            WHERE p.status = :status
              AND p.dataHoraInicio >= :dataInicio
              AND p.dataHoraInicio <= :dataFim
            GROUP BY p.motivo.id, p.motivo.categoria, p.motivo.subcategoria
            ORDER BY COALESCE(SUM(p.duracaoMinutos), 0) DESC
            """)
    List<TempoParadoPorMotivo> buscarTempoTotalParadoPorMotivo(
            @Param("status") StatusParada status,
            @Param("dataInicio") Instant dataInicio,
            @Param("dataFim") Instant dataFim
    );

    /** US30 — mesma coisa, escopada a um equipamento. Método separado (não parâmetro
     * opcional) pra não reintroduzir o padrão "IS NULL" que já causou problema na US08. */
    @Query("""
            SELECT p.motivo.id AS motivoId,
                   p.motivo.categoria AS categoria,
                   p.motivo.subcategoria AS subcategoria,
                   COALESCE(SUM(p.duracaoMinutos), 0) AS tempoTotalMinutos
            FROM Parada p
            WHERE p.status = :status
              AND p.equipamento.id = :equipamentoId
              AND p.dataHoraInicio >= :dataInicio
              AND p.dataHoraInicio <= :dataFim
            GROUP BY p.motivo.id, p.motivo.categoria, p.motivo.subcategoria
            ORDER BY COALESCE(SUM(p.duracaoMinutos), 0) DESC
            """)
    List<TempoParadoPorMotivo> buscarTempoTotalParadoPorMotivoDoEquipamento(
            @Param("status") StatusParada status,
            @Param("equipamentoId") UUID equipamentoId,
            @Param("dataInicio") Instant dataInicio,
            @Param("dataFim") Instant dataFim
    );
}
