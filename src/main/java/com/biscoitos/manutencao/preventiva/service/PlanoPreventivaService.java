package com.biscoitos.manutencao.preventiva.service;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.ordemservico.repository.OrdemServicoRepository;
import com.biscoitos.manutencao.preventiva.domain.PlanoPreventiva;
import com.biscoitos.manutencao.preventiva.repository.PlanoPreventivaRepository;
import com.biscoitos.manutencao.preventiva.repository.PlanoPreventivaSpecifications;
import com.biscoitos.manutencao.preventiva.web.dto.CriarPlanoPreventivaRequest;
import com.biscoitos.manutencao.preventiva.web.dto.RegistrarExecucaoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Dependência de ordemservico.repository.OrdemServicoRepository é intencional: preventiva
 * depende de ordemservico (só pra validar que um ordemServicoId informado na US20 existe
 * de verdade), nunca o contrário. Mesma direção estabelecida entre ordemservico e paradas
 * no Sprint 3 — sem ciclo entre módulos de negócio.
 */
@Service
@RequiredArgsConstructor
public class PlanoPreventivaService {

    private static final String ENTIDADE_AUDITORIA = "plano_preventiva";

    private final PlanoPreventivaRepository planoPreventivaRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public PlanoPreventiva criar(CriarPlanoPreventivaRequest request, UUID usuarioId) {
        // RN08
        if (request.intervaloDias() == null && request.intervaloHoras() == null) {
            throw new DadosInvalidosException(
                    "Informe intervaloDias e/ou intervaloHoras para o plano preventivo");
        }

        Equipamento equipamento = equipamentoRepository.findById(request.equipamentoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", request.equipamentoId()));

        PlanoPreventiva plano = PlanoPreventiva.builder()
                .equipamento(equipamento)
                .titulo(request.titulo())
                .descricao(request.descricao())
                .intervaloDias(request.intervaloDias())
                .intervaloHoras(request.intervaloHoras())
                .ativo(true)
                .dataCriacao(Instant.now())
                .build();

        PlanoPreventiva salvo = planoPreventivaRepository.save(plano);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salvo.getId(), usuarioId, "CREATE");
        return salvo;
    }

    @Transactional
    public PlanoPreventiva registrarExecucao(UUID planoId, RegistrarExecucaoRequest request, UUID usuarioId) {
        PlanoPreventiva plano = buscarPorId(planoId);

        if (request.ordemServicoId() != null && !ordemServicoRepository.existsById(request.ordemServicoId())) {
            throw new EntidadeNaoEncontradaException("Ordem de serviço", request.ordemServicoId());
        }

        plano.setDataUltimaExecucao(Instant.now());
        plano.setHorasNaUltimaExecucao(plano.getEquipamento().getHorasOperacaoAcumuladas());
        plano.setUltimaOrdemServicoId(request.ordemServicoId());

        PlanoPreventiva salvo = planoPreventivaRepository.save(plano);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salvo.getId(), usuarioId, "EXECUTE");
        return salvo;
    }

    @Transactional
    public PlanoPreventiva ativar(UUID planoId, UUID usuarioId) {
        PlanoPreventiva plano = buscarPorId(planoId);
        plano.setAtivo(true);
        PlanoPreventiva salvo = planoPreventivaRepository.save(plano);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salvo.getId(), usuarioId, "ACTIVATE");
        return salvo;
    }

    @Transactional
    public PlanoPreventiva inativar(UUID planoId, UUID usuarioId) {
        PlanoPreventiva plano = buscarPorId(planoId);
        plano.setAtivo(false);
        PlanoPreventiva salvo = planoPreventivaRepository.save(plano);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salvo.getId(), usuarioId, "DEACTIVATE");
        return salvo;
    }

    /**
     * "apenasVencidos" é aplicado em Java, depois da query (ver justificativa em
     * PlanoPreventivaSpecifications) — equipamentoId/ativo continuam filtrados no banco.
     */
    public List<PlanoPreventiva> listarComFiltros(UUID equipamentoId, Boolean ativo, boolean apenasVencidos) {
        Specification<PlanoPreventiva> spec = PlanoPreventivaSpecifications.comFiltros(equipamentoId, ativo);
        List<PlanoPreventiva> planos = planoPreventivaRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "titulo"));

        if (!apenasVencidos) {
            return planos;
        }
        return planos.stream().filter(this::estaVencido).toList();
    }

    public PlanoPreventiva buscarPorId(UUID id) {
        return planoPreventivaRepository.buscarComEquipamentoPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Plano preventivo", id));
    }

    /**
     * Critério de "vencido" (RF — alertas para revisões): vencido por QUALQUER critério
     * configurado que tiver sido atingido. Nunca executado conta como vencido também.
     */
    public boolean estaVencido(PlanoPreventiva plano) {
        if (plano.getIntervaloDias() != null) {
            if (plano.getDataUltimaExecucao() == null) {
                return true;
            }
            long diasDesdeExecucao = ChronoUnit.DAYS.between(plano.getDataUltimaExecucao(), Instant.now());
            if (diasDesdeExecucao >= plano.getIntervaloDias()) {
                return true;
            }
        }

        if (plano.getIntervaloHoras() != null) {
            if (plano.getHorasNaUltimaExecucao() == null) {
                return true;
            }
            int horasAtuais = plano.getEquipamento().getHorasOperacaoAcumuladas();
            int horasDesdeExecucao = horasAtuais - plano.getHorasNaUltimaExecucao();
            if (horasDesdeExecucao >= plano.getIntervaloHoras()) {
                return true;
            }
        }

        return false;
    }
}
