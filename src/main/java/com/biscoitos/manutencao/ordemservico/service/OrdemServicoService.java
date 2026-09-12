package com.biscoitos.manutencao.ordemservico.service;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.ordemservico.domain.OrdemServico;
import com.biscoitos.manutencao.ordemservico.domain.StatusOrdemServico;
import com.biscoitos.manutencao.ordemservico.repository.OrdemServicoRepository;
import com.biscoitos.manutencao.ordemservico.repository.OrdemServicoSpecifications;
import com.biscoitos.manutencao.ordemservico.service.exception.OrdemServicoEmEstadoTerminalException;
import com.biscoitos.manutencao.ordemservico.service.exception.OrdemServicoNaoEmAndamentoException;
import com.biscoitos.manutencao.ordemservico.service.exception.TecnicoNaoAtribuidoException;
import com.biscoitos.manutencao.ordemservico.web.dto.AbrirOrdemServicoRequest;
import com.biscoitos.manutencao.ordemservico.web.dto.ConcluirOrdemServicoRequest;
import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.repository.ParadaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Dependência de paradas.repository.ParadaRepository aqui é intencional e documentada:
 * ordemservico depende de paradas (pra vincular uma OS a uma parada de origem, US11),
 * nunca o contrário — paradas continua sem saber que ordemservico existe. Mesma direção
 * de dependência estabelecida no ADR-001 (core é a base; módulos de negócio podem
 * depender de outros módulos de negócio, desde que não formem ciclo).
 */
@Service
@RequiredArgsConstructor
public class OrdemServicoService {

    private static final String ENTIDADE_AUDITORIA = "ordem_servico";

    private final OrdemServicoRepository ordemServicoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParadaRepository paradaRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public OrdemServico abrir(AbrirOrdemServicoRequest request, UUID solicitanteId) {
        if (request.equipamentoId() == null && request.paradaId() == null) {
            throw new DadosInvalidosException("Informe equipamentoId ou paradaId para abrir a OS");
        }

        Equipamento equipamento;
        Parada paradaOrigem = null;

        if (request.paradaId() != null) {
            paradaOrigem = paradaRepository.buscarComRelacionamentosPorId(request.paradaId())
                    .orElseThrow(() -> new EntidadeNaoEncontradaException("Parada", request.paradaId()));
            // Equipamento vem da própria parada — evita abrir uma OS pra um equipamento
            // diferente do que realmente parou, se o campo equipamentoId vier preenchido
            // por engano junto com paradaId.
            equipamento = paradaOrigem.getEquipamento();
        } else {
            equipamento = equipamentoRepository.findById(request.equipamentoId())
                    .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", request.equipamentoId()));
        }

        Usuario solicitante = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", solicitanteId));

        OrdemServico os = OrdemServico.builder()
                .equipamento(equipamento)
                .titulo(request.titulo())
                .descricao(request.descricao())
                .tipo(request.tipo())
                .prioridade(request.prioridade())
                .status(StatusOrdemServico.ABERTA)
                .solicitante(solicitante)
                .dataAbertura(Instant.now())
                .build();

        OrdemServico salva = ordemServicoRepository.save(os);

        if (paradaOrigem != null) {
            paradaOrigem.setOrdemServicoId(salva.getId());
            paradaRepository.save(paradaOrigem);
        }

        auditoriaService.registrar(ENTIDADE_AUDITORIA, salva.getId(), solicitanteId, "CREATE");
        return salva;
    }

    @Transactional
    public OrdemServico atribuirTecnico(UUID osId, UUID tecnicoId, UUID usuarioId) {
        OrdemServico os = buscarPorId(osId);
        garantirNaoTerminal(os);

        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", tecnicoId));

        os.setTecnicoResponsavel(tecnico);
        OrdemServico salva = ordemServicoRepository.save(os);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salva.getId(), usuarioId, "ASSIGN");
        return salva;
    }

    @Transactional
    public OrdemServico iniciarExecucao(UUID osId, UUID usuarioId) {
        OrdemServico os = buscarPorId(osId);
        garantirNaoTerminal(os);

        // RN05
        if (os.getTecnicoResponsavel() == null) {
            throw new TecnicoNaoAtribuidoException(osId);
        }

        os.setStatus(StatusOrdemServico.EM_ANDAMENTO);
        os.setDataInicioExecucao(Instant.now());
        OrdemServico salva = ordemServicoRepository.save(os);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salva.getId(), usuarioId, "START");
        return salva;
    }

    @Transactional
    public OrdemServico concluir(UUID osId, ConcluirOrdemServicoRequest request, UUID usuarioId) {
        OrdemServico os = buscarPorId(osId);
        garantirNaoTerminal(os);

        // RN06
        if (os.getStatus() != StatusOrdemServico.EM_ANDAMENTO) {
            throw new OrdemServicoNaoEmAndamentoException(osId);
        }

        os.setStatus(StatusOrdemServico.CONCLUIDA);
        os.setDataConclusao(Instant.now());
        os.setObservacoesEncerramento(request.observacoes());
        OrdemServico salva = ordemServicoRepository.save(os);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salva.getId(), usuarioId, "COMPLETE");
        return salva;
    }

    @Transactional
    public OrdemServico cancelar(UUID osId, UUID usuarioId) {
        OrdemServico os = buscarPorId(osId);
        garantirNaoTerminal(os);

        os.setStatus(StatusOrdemServico.CANCELADA);
        os.setDataConclusao(Instant.now());
        OrdemServico salva = ordemServicoRepository.save(os);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salva.getId(), usuarioId, "CANCEL");
        return salva;
    }

    public List<OrdemServico> listarComFiltros(StatusOrdemServico status, UUID equipamentoId, UUID tecnicoId,
                                                Instant dataInicio, Instant dataFim) {
        Specification<OrdemServico> spec = OrdemServicoSpecifications.comFiltros(
                status, equipamentoId, tecnicoId, dataInicio, dataFim);
        return ordemServicoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dataAbertura"));
    }

    public OrdemServico buscarPorId(UUID id) {
        return ordemServicoRepository.buscarComRelacionamentosPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Ordem de serviço", id));
    }

    /** RN07 */
    private void garantirNaoTerminal(OrdemServico os) {
        if (os.getStatus() == StatusOrdemServico.CONCLUIDA || os.getStatus() == StatusOrdemServico.CANCELADA) {
            throw new OrdemServicoEmEstadoTerminalException(os.getId(), os.getStatus());
        }
    }
}
