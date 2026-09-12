package com.biscoitos.manutencao.ordemservico.repository;

import com.biscoitos.manutencao.ordemservico.domain.OrdemServico;
import com.biscoitos.manutencao.ordemservico.domain.StatusOrdemServico;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * US16. Mesmo padrão da US08 (ParadaSpecifications) — Criteria API, não JPQL com
 * "(:param IS NULL OR ...)". Essa segunda decisão já nasce sabendo por quê, não por
 * tentativa e erro: o padrão JPQL quebrou duas vezes contra o driver do Postgres com
 * parâmetros de data opcionalmente nulos (ver histórico completo em ParadaRepository).
 */
public final class OrdemServicoSpecifications {

    private OrdemServicoSpecifications() {
    }

    public static Specification<OrdemServico> comFiltros(StatusOrdemServico status, UUID equipamentoId,
                                                           UUID tecnicoId, Instant dataInicio, Instant dataFim) {
        return (root, query, cb) -> {
            root.fetch("equipamento", JoinType.INNER);
            root.fetch("solicitante", JoinType.INNER);
            root.fetch("tecnicoResponsavel", JoinType.LEFT);
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (equipamentoId != null) {
                predicates.add(cb.equal(root.get("equipamento").get("id"), equipamentoId));
            }
            if (tecnicoId != null) {
                predicates.add(cb.equal(root.get("tecnicoResponsavel").get("id"), tecnicoId));
            }
            if (dataInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataAbertura"), dataInicio));
            }
            if (dataFim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataAbertura"), dataFim));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
