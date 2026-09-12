package com.biscoitos.manutencao.ordemservico.repository;

import com.biscoitos.manutencao.ordemservico.domain.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, UUID>, JpaSpecificationExecutor<OrdemServico> {

    /**
     * LEFT JOIN em tecnicoResponsavel (não JOIN normal): esse campo é nullable — uma OS
     * recém-aberta ainda não tem técnico atribuído (US11 acontece antes da US12).
     */
    @Query("""
            SELECT os FROM OrdemServico os
            JOIN FETCH os.equipamento
            JOIN FETCH os.solicitante
            LEFT JOIN FETCH os.tecnicoResponsavel
            WHERE os.id = :id
            """)
    Optional<OrdemServico> buscarComRelacionamentosPorId(UUID id);
}
