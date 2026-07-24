package com.biscoitos.manutencao.paradas.service;

import com.biscoitos.manutencao.paradas.domain.MotivoParada;
import com.biscoitos.manutencao.paradas.repository.MotivoParadaRepository;
import com.biscoitos.manutencao.paradas.web.dto.MotivoParadaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotivoParadaService {

    private final MotivoParadaRepository motivoParadaRepository;

    public MotivoParada criar(MotivoParadaRequest request) {
        MotivoParada motivo = MotivoParada.builder()
                .categoria(request.categoria())
                .subcategoria(request.subcategoria())
                .descricao(request.descricao())
                .build();
        return motivoParadaRepository.save(motivo);
    }

    public List<MotivoParada> listar() {
        return motivoParadaRepository.findAll();
    }
}
