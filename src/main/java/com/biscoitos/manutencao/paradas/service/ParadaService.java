package com.biscoitos.manutencao.paradas.service;

import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.paradas.domain.MotivoParada;
import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.domain.StatusParada;
import com.biscoitos.manutencao.paradas.event.ParadaEventoTempoReal;
import com.biscoitos.manutencao.paradas.repository.MotivoParadaRepository;
import com.biscoitos.manutencao.paradas.repository.ParadaRepository;
import com.biscoitos.manutencao.paradas.repository.ParadaSpecifications;
import com.biscoitos.manutencao.paradas.repository.TempoParadoPorEquipamento;
import com.biscoitos.manutencao.paradas.repository.TempoParadoPorMotivo;
import com.biscoitos.manutencao.paradas.service.exception.IntervaloInvalidoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaEmAbertoException;
import com.biscoitos.manutencao.paradas.service.exception.ParadaJaEncerradaException;
import com.biscoitos.manutencao.paradas.web.dto.AbrirParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.EditarParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.EncerrarParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.ParadaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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

    private static final String ENTIDADE_AUDITORIA = "parada";

    private final ParadaRepository paradaRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final MotivoParadaRepository motivoParadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final ApplicationEventPublisher eventPublisher;

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

        Parada salva = paradaRepository.save(parada);
        // ParadaResponse montado AQUI, ainda dentro da transação — ver justificativa
        // detalhada no comentário de ParadaEventoTempoReal.
        eventPublisher.publishEvent(ParadaEventoTempoReal.aberta(ParadaResponse.from(salva)));
        return salva;
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

        Parada salva = paradaRepository.save(parada);
        eventPublisher.publishEvent(ParadaEventoTempoReal.encerrada(ParadaResponse.from(salva)));
        return salva;
    }

    /**
     * US09. RF06 + RNF03: corrige um apontamento já registrado, sempre gravando um
     * log de auditoria (quem, quando, o quê) — primeira vez que log_auditoria é
     * realmente usada (a tabela existe desde o Sprint 1, V1).
     */
    @Transactional
    public Parada editarParada(UUID paradaId, EditarParadaRequest request, UUID usuarioId) {
        Parada parada = paradaRepository.buscarComRelacionamentosPorId(paradaId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Parada", paradaId));

        MotivoParada motivo = motivoParadaRepository.findById(request.motivoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Motivo de parada", request.motivoId()));

        if (request.dataHoraFim() != null && parada.getStatus() == StatusParada.ABERTA) {
            throw new IntervaloInvalidoException(
                    "Não é possível definir data_hora_fim em uma parada ainda ABERTA; "
                            + "use PATCH /api/paradas/{id}/encerrar para isso"
            );
        }

        Instant novoInicio = request.dataHoraInicio();
        // Se dataHoraFim não veio na requisição, preserva o valor que já existia
        // (evita apagar um fim já registrado só porque o campo não foi reenviado).
        Instant novoFim = request.dataHoraFim() != null ? request.dataHoraFim() : parada.getDataHoraFim();

        // RN01 continua valendo numa correção, não só na abertura/encerramento original.
        if (novoFim != null && novoFim.isBefore(novoInicio)) {
            throw new IntervaloInvalidoException(
                    "data_hora_fim (" + novoFim + ") não pode ser anterior a data_hora_inicio (" + novoInicio + ")"
            );
        }

        parada.setMotivo(motivo);
        parada.setObservacoes(request.observacoes());
        parada.setDataHoraInicio(novoInicio);
        if (novoFim != null) {
            parada.setDataHoraFim(novoFim);
            parada.setDuracaoMinutos(Duration.between(novoInicio, novoFim).toMinutes());
        }

        Parada salva = paradaRepository.save(parada);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salva.getId(), usuarioId, "UPDATE");
        return salva;
    }

    public List<Parada> listarComFiltros(UUID equipamentoId, UUID motivoId, UUID responsavelId,
                                          Instant dataInicio, Instant dataFim) {
        Specification<Parada> spec = ParadaSpecifications.comFiltros(
                equipamentoId, motivoId, responsavelId, dataInicio, dataFim);
        return paradaRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dataHoraInicio"));
    }

    /**
     * US10 (RF07). dataInicio/dataFim nulos viram "sem limite inferior" / "até agora" —
     * resolvidos AQUI, antes de chamar o repository, exatamente pra nunca passar um
     * parâmetro de data nulo pro JPQL (ver o porquê no comentário do repository).
     */
    public List<TempoParadoPorEquipamento> tempoTotalParadoPorEquipamento(Instant dataInicio, Instant dataFim) {
        Instant inicioEfetivo = dataInicio != null ? dataInicio : Instant.EPOCH;
        Instant fimEfetivo = dataFim != null ? dataFim : Instant.now();
        return paradaRepository.buscarTempoTotalParadoPorEquipamento(
                StatusParada.ENCERRADA, inicioEfetivo, fimEfetivo);
    }

    public Parada buscarPorId(UUID id) {
        return paradaRepository.buscarComRelacionamentosPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Parada", id));
    }

    /** US29 (Dashboard de OEE). equipamentoId/dataInicio/dataFim sempre concretos — quem
     * chama (DashboardService) exige o período explícito, é o próprio "tempo planejado". */
    public long tempoParadoDoEquipamentoNoPeriodo(UUID equipamentoId, Instant dataInicio, Instant dataFim) {
        return paradaRepository.somarTempoParadoDoEquipamentoNoPeriodo(
                StatusParada.ENCERRADA, equipamentoId, dataInicio, dataFim);
    }

    /**
     * US30. Mesmo padrão de sentinela da US10 pras datas. equipamentoId nulo = ranking
     * de todos os equipamentos (usa o método sem filtro); informado = escopado a um só
     * (método separado, não parâmetro opcional — evita reintroduzir "IS NULL" na query).
     */
    public List<TempoParadoPorMotivo> principaisCausasParada(UUID equipamentoId, Instant dataInicio, Instant dataFim) {
        Instant inicioEfetivo = dataInicio != null ? dataInicio : Instant.EPOCH;
        Instant fimEfetivo = dataFim != null ? dataFim : Instant.now();

        if (equipamentoId != null) {
            return paradaRepository.buscarTempoTotalParadoPorMotivoDoEquipamento(
                    StatusParada.ENCERRADA, equipamentoId, inicioEfetivo, fimEfetivo);
        }
        return paradaRepository.buscarTempoTotalParadoPorMotivo(StatusParada.ENCERRADA, inicioEfetivo, fimEfetivo);
    }
}
