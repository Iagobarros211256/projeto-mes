package com.biscoitos.manutencao.producao.repository;

import com.biscoitos.manutencao.producao.domain.ApontamentoProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ApontamentoProducaoRepository extends JpaRepository<ApontamentoProducao, UUID>, JpaSpecificationExecutor<ApontamentoProducao> {

    @Query("SELECT a FROM ApontamentoProducao a JOIN FETCH a.equipamento JOIN FETCH a.responsavel WHERE a.id = :id")
    Optional<ApontamentoProducao> buscarComRelacionamentosPorId(UUID id);

    /**
     * Usada pelo Dashboard (US29). dataInicio/dataFim aqui SEMPRE vêm concretos (o
     * endpoint de OEE exige o período explícito, "tempo planejado" não existe sem
     * um período definido) — não precisa dos sentinelas usados em US10/US30 pra
     * filtros opcionais.
     */
    @Query("""
            SELECT COALESCE(SUM(a.quantidadeProduzida), 0) AS totalProduzido,
                   COALESCE(SUM(a.quantidadeBoa), 0) AS totalBoa
            FROM ApontamentoProducao a
            WHERE a.equipamento.id = :equipamentoId
              AND a.dataHoraInicio >= :dataInicio
              AND a.dataHoraInicio <= :dataFim
            """)
    TotaisProducao somarProducaoDoEquipamentoNoPeriodo(UUID equipamentoId, Instant dataInicio, Instant dataFim);
}
