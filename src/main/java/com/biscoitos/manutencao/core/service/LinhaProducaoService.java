package com.biscoitos.manutencao.core.service;

import com.biscoitos.manutencao.core.domain.LinhaProducao;
import com.biscoitos.manutencao.core.repository.LinhaProducaoRepository;
import com.biscoitos.manutencao.core.web.dto.LinhaProducaoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LinhaProducaoService {

    private final LinhaProducaoRepository linhaProducaoRepository;

    public LinhaProducao criar(LinhaProducaoRequest request) {
        LinhaProducao linha = LinhaProducao.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .build();
        return linhaProducaoRepository.save(linha);
    }

    public List<LinhaProducao> listar() {
        return linhaProducaoRepository.findAll();
    }
}
