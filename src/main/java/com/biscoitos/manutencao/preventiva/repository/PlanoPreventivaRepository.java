package com.biscoitos.manutencao.preventiva.repository;

import com.biscoitos.manutencao.preventiva.domain.PlanoPreventiva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PlanoPreventivaRepository extends JpaRepository<PlanoPreventiva, UUID>, JpaSpecificationExecutor<PlanoPreventiva> {

    @Query("SELECT p FROM PlanoPreventiva p JOIN FETCH p.equipamento WHERE p.id = :id")
    Optional<PlanoPreventiva> buscarComEquipamentoPorId(UUID id);
}
