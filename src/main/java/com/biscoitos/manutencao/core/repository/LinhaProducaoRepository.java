package com.biscoitos.manutencao.core.repository;

import com.biscoitos.manutencao.core.domain.LinhaProducao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LinhaProducaoRepository extends JpaRepository<LinhaProducao, UUID> {
}
