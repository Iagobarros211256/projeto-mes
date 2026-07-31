package com.biscoitos.manutencao.paradas.service;

import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.paradas.domain.MotivoParada;
import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.domain.StatusParada;
import com.biscoitos.manutencao.paradas.repository.MotivoParadaRepository;
import com.biscoitos.manutencao.paradas.repository.ParadaRepository;
import com.biscoitos.manutencao.paradas.repository.ParadaSpecifications;
import com.biscoitos.manutencao.paradas.service.exception.IntervaloInvalidoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaEmAbertoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaJaEncerradaException;
import com.biscoitos.manutencao.paradas.web.dto.AbrirParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.EncerrarParadaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParadaService {

    private final ParadaRepository paradaRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final MotivoParadaRepository motivoParadaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Parada abrirParada(AbrirParadaRequest request, UUID responsavelId) {
        Equipamento equipamento = equipamentoRepository.findById(request.equipamentoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", request.equipamentoId()));

        MotivoParada motivo = motivoParadaRepository.findById(request.motivoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Motivo de parada", request.motivoId()));

        Usuario responsavel = usuarioRepository.findById(responsavelId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", responsavelId));

        // RN02 — um equipamento não pode ter duas paradas ABERTA simultaneamente.
        if (paradaRepository.existsByEquipamentoIdAndStatus(equipamento.getId(), StatusParada.ABERTA)) {
            throw new ParadaEmAbertoException(equipamento.getId());
        }

        // RN03 (parada sem motivo) já é garantida pelo @NotNull em AbrirParadaRequest.motivoId,
        // reforçado aqui pela busca acima, que falha explicitamente se o motivo não existir.

        Parada parada = Parada.builder()
                .equipamento(equipamento)
                .motivo(motivo)
                .responsavel(responsavel)
                .dataHoraInicio(Instant.now())
                .observacoes(request.observacoes())
                .status(StatusParada.ABERTA)
                .build();

        return paradaRepository.save(parada);
    }

    @Transactional
    public Parada encerrarParada(UUID paradaId, EncerrarParadaRequest request) {
        // Usa a query com JOIN FETCH (não um findById simples): o Controller vai montar o
        // ParadaResponse com o objeto retornado aqui, já fora da transação (open-in-view=false).
        Parada parada = paradaRepository.buscarComRelacionamentosPorId(paradaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Parada", paradaId));

        if (parada.getStatus() == StatusParada.ENCERRADA) {
            throw new ParadaJaEncerradaException(paradaId);
        }

        Instant fim = request.dataHoraFim() != null ? request.dataHoraFim() : Instant.now();

        // RN01 — não pode encerrar com fim anterior ao início.
        if (fim.isBefore(parada.getDataHoraInicio())) {
            throw new IntervaloInvalidoException(
                    "data_hora_fim (" + fim + ") não pode ser anterior a data_hora_inicio (" + parada.getDataHoraInicio() + ")"
            );
        }

        long duracaoMinutos = Duration.between(parada.getDataHoraInicio(), fim).toMinutes();

        parada.setDataHoraFim(fim);
        parada.setDuracaoMinutos(duracaoMinutos);
        parada.setStatus(StatusParada.ENCERRADA);

        return paradaRepository.save(parada);
    }

    public List<Parada> listarComFiltros(UUID equipamentoId, UUID motivoId, UUID responsavelId,
                                          Instant dataInicio, Instant dataFim) {
        Specification<Parada> spec = ParadaSpecifications.comFiltros(
                equipamentoId, motivoId, responsavelId, dataInicio, dataFim);
        return paradaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dataHoraInicio"));
    }

    public Parada buscarPorId(UUID id) {
        return paradaRepository.buscarComRelacionamentosPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Parada", id));
    }
}
