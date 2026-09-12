package com.biscoitos.manutencao.estoque.repository;

import com.biscoitos.manutencao.estoque.domain.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID>, JpaSpecificationExecutor<MovimentacaoEstoque> {
}
