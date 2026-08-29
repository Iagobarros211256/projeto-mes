package com.biscoitos.manutencao.preventiva.repository;

import com.biscoitos.manutencao.preventiva.domain.PlanoPreventiva;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "Vencido" não é filtro de banco de propósito: depende de comparar contra "agora" e
 * contra o horômetro ATUAL do equipamento, ao mesmo tempo, com regra de OR entre dois
 * critérios independentes por linha — dá pra fazer em SQL, mas seria mais uma query
 * candidata a reproduzir o tipo de problema que já penou a US08. Como o volume de planos
 * preventivos é pequeno (não é uma tabela que cresce por evento, como parada), calcular
 * em Java depois de buscar é mais simples e não tem esse risco.
 */
public final class PlanoPreventivaSpecifications {

    private PlanoPreventivaSpecifications() {
    }

    public static Specification<PlanoPreventiva> comFiltros(UUID equipamentoId, Boolean ativo) {
        return (root, query, cb) -> {
            root.fetch("equipamento", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            if (equipamentoId != null) {
                predicates.add(cb.equal(root.get("equipamento").get("id"), equipamentoId));
            }
            if (ativo != null) {
                predicates.add(cb.equal(root.get("ativo"), ativo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
