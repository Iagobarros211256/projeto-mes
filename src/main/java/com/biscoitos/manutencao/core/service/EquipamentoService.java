package com.biscoitos.manutencao.core.service;

import com.biscoitos.manutencao.common.exception.DuplicidadeException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.LinhaProducao;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.LinhaProducaoRepository;
import com.biscoitos.manutencao.core.web.dto.EquipamentoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final LinhaProducaoRepository linhaProducaoRepository;

    public Equipamento criar(EquipamentoRequest request) {
        if (equipamentoRepository.existsByCodigo(request.codigo())) {
            throw new DuplicidadeException("Já existe um equipamento com o código " + request.codigo());
        }

        LinhaProducao linha = linhaProducaoRepository.findById(request.linhaProducaoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Linha de produção", request.linhaProducaoId()));

        Equipamento equipamento = Equipamento.builder()
                .codigo(request.codigo())
                .nome(request.nome())
                .setor(request.setor())
                .linhaProducao(linha)
                .build();

        return equipamentoRepository.save(equipamento);
    }

    public List<Equipamento> listar() {
        return equipamentoRepository.findAllComLinhaProducao();
    }
}
