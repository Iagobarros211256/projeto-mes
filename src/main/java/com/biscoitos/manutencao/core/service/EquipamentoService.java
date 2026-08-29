package com.biscoitos.manutencao.core.service;

import com.biscoitos.manutencao.common.exception.DuplicidadeException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.common.exception.HorometroRetrocessoException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.LinhaProducao;
import com.biscoitos.manutencao.core.domain.StatusEquipamento;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.LinhaProducaoRepository;
import com.biscoitos.manutencao.core.web.dto.EquipamentoRequest;
import com.biscoitos.manutencao.core.web.dto.EquipamentoUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    @Transactional
    public Equipamento atualizar(UUID id, EquipamentoUpdateRequest request) {
        Equipamento equipamento = equipamentoRepository.findByIdComLinhaProducao(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", id));

        LinhaProducao linha = linhaProducaoRepository.findById(request.linhaProducaoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Linha de produção", request.linhaProducaoId()));

        equipamento.setNome(request.nome());
        equipamento.setSetor(request.setor());
        equipamento.setLinhaProducao(linha);

        return equipamentoRepository.save(equipamento);
    }

    @Transactional
    public Equipamento inativar(UUID id) {
        Equipamento equipamento = equipamentoRepository.findByIdComLinhaProducao(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", id));
        equipamento.setStatus(StatusEquipamento.INATIVO);
        return equipamentoRepository.save(equipamento);
    }

    @Transactional
    public Equipamento ativar(UUID id) {
        Equipamento equipamento = equipamentoRepository.findByIdComLinhaProducao(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", id));
        equipamento.setStatus(StatusEquipamento.ATIVO);
        return equipamentoRepository.save(equipamento);
    }

    /**
     * US18 (Preventiva). RN09 — o horômetro é cumulativo, como um odômetro: só aceita
     * valores maiores ou iguais ao já registrado.
     */
    @Transactional
    public Equipamento atualizarHorasOperacao(UUID id, int novasHoras) {
        Equipamento equipamento = equipamentoRepository.findByIdComLinhaProducao(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", id));

        int horasAtuais = equipamento.getHorasOperacaoAcumuladas();
        if (novasHoras < horasAtuais) {
            throw new HorometroRetrocessoException(id, horasAtuais, novasHoras);
        }

        equipamento.setHorasOperacaoAcumuladas(novasHoras);
        return equipamentoRepository.save(equipamento);
    }
}
