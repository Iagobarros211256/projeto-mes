package com.biscoitos.manutencao.producao.repository;

import com.biscoitos.manutencao.producao.domain.ApontamentoProducao;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Mesmo padrão de Criteria API já estabelecido em US08/US16/US26 — sem JPQL com filtros de data opcionalmente nulos. */
public final class ApontamentoProducaoSpecifications {

    private ApontamentoProducaoSpecifications() {
    }

    public static Specification<ApontamentoProducao> comFiltros(UUID equipamentoId, Instant dataInicio, Instant dataFim) {
        return (root, query, cb) -> {
            root.fetch("equipamento", JoinType.INNER);
            root.fetch("responsavel", JoinType.INNER);
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (equipamentoId != null) {
                predicates.add(cb.equal(root.get("equipamento").get("id"), equipamentoId));
            }
            if (dataInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataHoraInicio"), dataInicio));
            }
            if (dataFim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataHoraInicio"), dataFim));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
