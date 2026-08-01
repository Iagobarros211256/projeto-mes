package com.biscoitos.manutencao.core.repository;

import com.biscoitos.manutencao.core.domain.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {

    /**
     * Não é usado pela US09 diretamente (que só grava o log), mas é a consulta óbvia
     * que qualquer tela futura de "histórico de alterações" vai precisar — barato de
     * deixar pronto agora.
     */
    List<LogAuditoria> findByEntidadeAndEntidadeIdOrderByDataHoraDesc(String entidade, UUID entidadeId);
}
