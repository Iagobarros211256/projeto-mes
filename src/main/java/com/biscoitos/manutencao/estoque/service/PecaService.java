package com.biscoitos.manutencao.estoque.service;

import com.biscoitos.manutencao.common.exception.DuplicidadeException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.estoque.domain.MovimentacaoEstoque;
import com.biscoitos.manutencao.estoque.domain.Peca;
import com.biscoitos.manutencao.estoque.domain.TipoMovimentacao;
import com.biscoitos.manutencao.estoque.repository.MovimentacaoEstoqueRepository;
import com.biscoitos.manutencao.estoque.repository.MovimentacaoEstoqueSpecifications;
import com.biscoitos.manutencao.estoque.repository.PecaRepository;
import com.biscoitos.manutencao.estoque.service.exception.EstoqueInsuficienteException;
import com.biscoitos.manutencao.estoque.web.dto.CriarPecaRequest;
import com.biscoitos.manutencao.estoque.web.dto.RegistrarMovimentacaoRequest;
import com.biscoitos.manutencao.ordemservico.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Dependência de ordemservico.repository.OrdemServicoRepository é intencional, mesma
 * direção já estabelecida entre preventiva e ordemservico no Sprint 4: estoque depende
 * de ordemservico (só pra validar que um ordemServicoId informado na US24 existe de
 * verdade), nunca o contrário.
 */
@Service
@RequiredArgsConstructor
public class PecaService {

    private static final String ENTIDADE_AUDITORIA = "peca";

    private final PecaRepository pecaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public Peca criar(CriarPecaRequest request, UUID usuarioId) {
        if (pecaRepository.existsByCodigo(request.codigo())) {
            throw new DuplicidadeException("Já existe uma peça com o código " + request.codigo());
        }

        Peca peca = Peca.builder()
                .codigo(request.codigo())
                .nome(request.nome())
                .descricao(request.descricao())
                .unidadeMedida(request.unidadeMedida())
                .quantidadeAtual(0)
                .estoqueMinimo(request.estoqueMinimo())
                .dataCriacao(Instant.now())
                .build();

        Peca salva = pecaRepository.save(peca);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, salva.getId(), usuarioId, "CREATE");
        return salva;
    }

    @Transactional
    public MovimentacaoEstoque registrarEntrada(UUID pecaId, RegistrarMovimentacaoRequest request, UUID usuarioId) {
        Peca peca = buscarPecaPorId(pecaId);
        MovimentacaoEstoque mov = registrarMovimentacao(peca, TipoMovimentacao.ENTRADA, request, usuarioId);
        peca.setQuantidadeAtual(peca.getQuantidadeAtual() + request.quantidade());
        pecaRepository.save(peca);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, peca.getId(), usuarioId, "ENTRADA");
        return mov;
    }

    @Transactional
    public MovimentacaoEstoque registrarSaida(UUID pecaId, RegistrarMovimentacaoRequest request, UUID usuarioId) {
        Peca peca = buscarPecaPorId(pecaId);

        // RN10
        int saldoAposSaida = peca.getQuantidadeAtual() - request.quantidade();
        if (saldoAposSaida < 0) {
            throw new EstoqueInsuficienteException(pecaId, peca.getQuantidadeAtual(), request.quantidade());
        }

        MovimentacaoEstoque mov = registrarMovimentacao(peca, TipoMovimentacao.SAIDA, request, usuarioId);
        peca.setQuantidadeAtual(saldoAposSaida);
        pecaRepository.save(peca);
        auditoriaService.registrar(ENTIDADE_AUDITORIA, peca.getId(), usuarioId, "SAIDA");
        return mov;
    }

    private MovimentacaoEstoque registrarMovimentacao(Peca peca, TipoMovimentacao tipo,
                                                        RegistrarMovimentacaoRequest request, UUID usuarioId) {
        if (request.ordemServicoId() != null && !ordemServicoRepository.existsById(request.ordemServicoId())) {
            throw new EntidadeNaoEncontradaException("Ordem de serviço", request.ordemServicoId());
        }

        Usuario responsavel = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Usuário", usuarioId));

        MovimentacaoEstoque mov = MovimentacaoEstoque.builder()
                .peca(peca)
                .tipo(tipo)
                .quantidade(request.quantidade())
                .observacao(request.observacao())
                .responsavel(responsavel)
                .dataHora(Instant.now())
                .ordemServicoId(request.ordemServicoId())
                .build();

        return movimentacaoEstoqueRepository.save(mov);
    }

    public List<Peca> listar(boolean apenasEstoqueBaixo) {
        List<Peca> pecas = pecaRepository.findAll();
        if (!apenasEstoqueBaixo) {
            return pecas;
        }
        return pecas.stream()
                .filter(p -> p.getQuantidadeAtual() <= p.getEstoqueMinimo())
                .toList();
    }

    public Peca buscarPecaPorId(UUID id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new EntidadeNaoEncontradaException("Peça", id));
    }

    public List<MovimentacaoEstoque> listarMovimentacoesComFiltros(UUID pecaId, TipoMovimentacao tipo,
                                                                     Instant dataInicio, Instant dataFim) {
        Specification<MovimentacaoEstoque> spec = MovimentacaoEstoqueSpecifications.comFiltros(
                pecaId, tipo, dataInicio, dataFim);
        return movimentacaoEstoqueRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "dataHora"));
    }
}
