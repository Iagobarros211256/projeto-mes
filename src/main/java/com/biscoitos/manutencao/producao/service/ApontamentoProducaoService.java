package com.biscoitos.manutencao.producao.service;

import com.biscoitos.manutencao.common.exception.DadosInvalidosException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.EquipamentoRepository;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.producao.domain.ApontamentoProducao;
import com.biscoitos.manutencao.producao.repository.ApontamentoProducaoRepository;
import com.biscoitos.manutencao.producao.repository.ApontamentoProducaoSpecifications;
import com.biscoitos.manutencao.producao.web.dto.RegistrarApontamentoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApontamentoProducaoService {

    private static final String ENTIDADE_AUDITORIA = "apontamento_producao";

    private final ApontamentoProducaoRepository apontamentoProducaoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public ApontamentoProducao registrar(RegistrarApontamentoRequest request, UUID usuarioId) {
        // RN13
        if (!request.dataHoraFim().isAfter(request.dataHoraInicio())) {
            throw new DadosInvalidosException(
                    "dataHoraFim (" + request.dataHoraFim() + ") precisa ser depois de dataHoraInicio ("
                            + request.dataHoraInicio() + ")");
        }
        // RN12
        if (request.quantidadeBoa() > request.quantidadeProduzida()) {
            throw new DadosInvalidosException(
                    "quantidadeBoa (" + request.quantidadeBoa() + ") não pode ser maior que quantidadeProduzida ("
                            + request.quantidadeProduzida() + ")");
        }

        Equipamento equipamento = equipamentoRepository.findById(request.equipamentoId())
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Equipamento", request.equipamentoId()));

        Usuario responsavel = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", usuarioId));

        ApontamentoProducao apontamento = ApontamentoProducao.builder()
                .equipamento(equipamento)
                .dataHoraInicio(request.dataHoraInicio())
                .dataHoraFim(request.dataHoraFim())
                .quantidadeProduzida(request.quantidadeProduzida())
                .quantidadeBoa(request.quantidadeBoa())
                .responsavel(responsavel)
                .dataRegistro(Instant.now())
                .build();

        ApontamentoProducao salvo = apontamentoProducaoRepository.save(apontamento);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salvo.getId(), usuarioId, "CREATE");
        return salvo;
    }

    public List<ApontamentoProducao> listarComFiltros(UUID equipamentoId, Instant dataInicio, Instant dataFim) {
        Specification<ApontamentoProducao> spec = ApontamentoProducaoSpecifications.comFiltros(
                equipamentoId, dataInicio, dataFim);
        return apontamentoProducaoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dataHoraInicio"));
    }

    public ApontamentoProducao buscarPorId(UUID id) {
        return apontamentoProducaoRepository.buscarComRelacionamentosPorId(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Apontamento de produção", id));
    }
}
