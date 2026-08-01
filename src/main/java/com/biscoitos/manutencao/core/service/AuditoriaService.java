package com.biscoitos.manutencao.core.service;

import com.biscoitos.manutencao.core.domain.LogAuditoria;
import com.biscoitos.manutencao.core.repository.LogAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    @Transactional
    public void registrar(String entidade, UUID entidadeId, UUID usuarioId, String acao) {
        LogAuditoria log = LogAuditoria.builder()
                .entidade(entidade)
                .entidadeId(entidadeId)
                .usuarioId(usuarioId)
                .acao(acao)
                .dataHora(Instant.now())
                .build();
        logAuditoriaRepository.save(log);
    }
}
