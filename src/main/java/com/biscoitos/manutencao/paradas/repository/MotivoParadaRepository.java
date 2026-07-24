package com.biscoitos.manutencao.paradas.repository;

import com.biscoitos.manutencao.paradas.domain.MotivoParada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MotivoParadaRepository extends JpaRepository<MotivoParada, UUID> {
}
