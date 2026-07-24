package com.biscoitos.manutencao.core.repository;

import com.biscoitos.manutencao.core.domain.Equipamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID> {
    boolean existsByCodigo(String codigo);

    /**
     * JOIN FETCH é necessário aqui: linhaProducao é LAZY, e como o projeto usa
     * open-in-view=false, a sessão do Hibernate fecha assim que o repository
     * retorna. Sem isso, o DTO (montado no Controller) tenta ler
     * linhaProducao.getNome() fora da sessão e quebra com LazyInitializationException.
     */
    @Query("SELECT e FROM Equipamento e JOIN FETCH e.linhaProducao")
    List<Equipamento> findAllComLinhaProducao();
}
