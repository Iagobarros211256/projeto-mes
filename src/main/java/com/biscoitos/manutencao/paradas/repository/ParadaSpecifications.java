package com.biscoitos.manutencao.paradas.repository;

import com.biscoitos.manutencao.paradas.domain.Parada;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ParadaSpecifications {

    private ParadaSpecifications() {
    }

    /**
     * US08. Cada filtro só vira predicado se não for nulo — ao contrário do padrão
     * "(:param IS NULL OR campo = :param)" em JPQL puro, que quebrou duas vezes contra
     * o driver do Postgres (erro de inferência de tipo, depois erro de CAST pra bytea).
     * Aqui o Java decide o que entra na query antes dela ser montada; o banco nunca vê
     * um parâmetro "opcional".
     */
    public static Specification<Parada> comFiltros(UUID equipamentoId, UUID motivoId, UUID responsavelId,
                                                     Instant dataInicio, Instant dataFim) {
        return (root, query, cb) -> {
            // JOIN FETCH via Criteria — necessário pelo mesmo motivo do Sprint 1
            // (LazyInitializationException fora da sessão, com open-in-view=false).
            root.fetch("equipamento", JoinType.INNER);
            root.fetch("motivo", JoinType.INNER);
            root.fetch("responsavel", JoinType.INNER);
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (equipamentoId != null) {
                predicates.add(cb.equal(root.get("equipamento").get("id"), equipamentoId));
            }
            if (motivoId != null) {
                predicates.add(cb.equal(root.get("motivo").get("id"), motivoId));
            }
            if (responsavelId != null) {
                predicates.add(cb.equal(root.get("responsavel").get("id"), responsavelId));
            }
            // Filtra por dataHoraInicio (quando a parada começou), não por dataHoraFim —
            // é o que responde "o que aconteceu nesse turno/período" (RF05).
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
