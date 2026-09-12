package com.biscoitos.manutencao.estoque.repository;

import com.biscoitos.manutencao.estoque.domain.MovimentacaoEstoque;
import com.biscoitos.manutencao.estoque.domain.TipoMovimentacao;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Mesmo padrão de ParadaSpecifications (US08) e OrdemServicoSpecifications (US16):
 * Criteria API, não JPQL com "(:param IS NULL OR ...)" — evita a classe de bug que já
 * penou a US08 duas vezes com parâmetros de data opcionalmente nulos contra o Postgres.
 */
public final class MovimentacaoEstoqueSpecifications {

    private MovimentacaoEstoqueSpecifications() {
    }

    public static Specification<MovimentacaoEstoque> comFiltros(UUID pecaId, TipoMovimentacao tipo,
                                                                  Instant dataInicio, Instant dataFim) {
        return (root, query, cb) -> {
            root.fetch("peca", JoinType.INNER);
            root.fetch("responsavel", JoinType.INNER);
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (pecaId != null) {
                predicates.add(cb.equal(root.get("peca").get("id"), pecaId));
            }
            if (tipo != null) {
                predicates.add(cb.equal(root.get("tipo"), tipo));
            }
            if (dataInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataHora"), dataInicio));
            }
            if (dataFim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataHora"), dataFim));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
