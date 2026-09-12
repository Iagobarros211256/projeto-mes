package com.biscoitos.manutencao.estoque.service;

import com.biscoitos.manutencao.common.exception.DuplicidadeException;
import com.biscoitos.manutencao.common.exception.EntidadeNaoEncontradaException;
import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.repository.UsuarioRepository;
import com.biscoitos.manutencao.core.service.AuditoriaService;
import com.biscoitos.manutencao.estoque.domain.MovimentacaoEstoque;
import com.biscoitos.manutencao.estoque.domain.Peca;
import com.biscoitos.manutencao.estoque.repository.MovimentacaoEstoqueRepository;
import com.biscoitos.manutencao.estoque.repository.PecaRepository;
import com.biscoitos.manutencao.estoque.service.exception.EstoqueInsuficienteException;
import com.biscoitos.manutencao.estoque.web.dto.CriarPecaRequest;
import com.biscoitos.manutencao.estoque.web.dto.RegistrarMovimentacaoRequest;
import com.biscoitos.manutencao.ordemservico.repository.OrdemServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PecaServiceTest {

    @Mock
    private PecaRepository pecaRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private OrdemServicoRepository ordemServicoRepository;
    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PecaService pecaService;

    @Test
    void deveRecusarCriarComCodigoDuplicado() {
        when(pecaRepository.existsByCodigo("ROL-6204")).thenReturn(true);

        CriarPecaRequest request = new CriarPecaRequest("ROL-6204", "Rolamento 6204", null, "UN", 5);

        assertThatThrownBy(() -> pecaService.criar(request, UUID.randomUUID()))
                .isInstanceOf(DuplicidadeException.class);
    }

    @Test
    void deveCriarPecaComQuantidadeZerada() {
        when(pecaRepository.existsByCodigo("ROL-6204")).thenReturn(false);
        when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

        CriarPecaRequest request = new CriarPecaRequest("ROL-6204", "Rolamento 6204", null, "UN", 5);

        Peca peca = pecaService.criar(request, UUID.randomUUID());

        assertThat(peca.getQuantidadeAtual()).isZero();
        assertThat(peca.getEstoqueMinimo()).isEqualTo(5);
    }

    @Test
    void deveRegistrarEntradaEAumentarEstoque() {
        UUID pecaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Peca peca = Peca.builder().id(pecaId).quantidadeAtual(10).build();

        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(Usuario.builder().id(usuarioId).nome("Bruno").build()));
        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarMovimentacaoRequest request = new RegistrarMovimentacaoRequest(20, "Compra", null);

        pecaService.registrarEntrada(pecaId, request, usuarioId);

        assertThat(peca.getQuantidadeAtual()).isEqualTo(30);
    }

    @Test
    void deveRegistrarSaidaEDiminuirEstoque() {
        UUID pecaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Peca peca = Peca.builder().id(pecaId).quantidadeAtual(10).build();

        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(Usuario.builder().id(usuarioId).nome("Bruno").build()));
        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrarMovimentacaoRequest request = new RegistrarMovimentacaoRequest(4, "Usado na OS-123", null);

        pecaService.registrarSaida(pecaId, request, usuarioId);

        assertThat(peca.getQuantidadeAtual()).isEqualTo(6);
    }

    @Test
    void deveRecusarSaidaQueDeixariaEstoqueNegativo() {
        // RN10
        UUID pecaId = UUID.randomUUID();
        Peca peca = Peca.builder().id(pecaId).quantidadeAtual(3).build();

        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));

        RegistrarMovimentacaoRequest request = new RegistrarMovimentacaoRequest(4, null, null);

        assertThatThrownBy(() -> pecaService.registrarSaida(pecaId, request, UUID.randomUUID()))
                .isInstanceOf(EstoqueInsuficienteException.class);
    }

    @Test
    void devePermitirSaidaQueZeraOEstoqueExatamente() {
        // Saída igual ao saldo não é "insuficiente" — RN10 só bloqueia ficar NEGATIVO.
        UUID pecaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Peca peca = Peca.builder().id(pecaId).quantidadeAtual(4).build();

        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));
        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(Usuario.builder().id(usuarioId).nome("Bruno").build()));
        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pecaRepository.save(any(Peca.class))).thenAnswer(inv -> inv.getArgument(0));

        pecaService.registrarSaida(pecaId, new RegistrarMovimentacaoRequest(4, null, null), usuarioId);

        assertThat(peca.getQuantidadeAtual()).isZero();
    }

    @Test
    void deveFalharAoMovimentarComOrdemServicoInexistente() {
        UUID pecaId = UUID.randomUUID();
        UUID osId = UUID.randomUUID();
        Peca peca = Peca.builder().id(pecaId).quantidadeAtual(10).build();

        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));
        when(ordemServicoRepository.existsById(osId)).thenReturn(false);

        RegistrarMovimentacaoRequest request = new RegistrarMovimentacaoRequest(1, null, osId);

        assertThatThrownBy(() -> pecaService.registrarSaida(pecaId, request, UUID.randomUUID()))
                .isInstanceOf(EntidadeNaoEncontradaException.class);
    }

    @Test
    void deveFiltrarApenasEstoqueBaixo() {
        Peca baixo = Peca.builder().id(UUID.randomUUID()).quantidadeAtual(2).estoqueMinimo(5).build();
        Peca ok = Peca.builder().id(UUID.randomUUID()).quantidadeAtual(20).estoqueMinimo(5).build();

        when(pecaRepository.findAll()).thenReturn(List.of(baixo, ok));

        List<Peca> resultado = pecaService.listar(true);

        assertThat(resultado).containsExactly(baixo);
    }

    @Test
    void semFiltroDevolveTodasAsPecas() {
        Peca baixo = Peca.builder().id(UUID.randomUUID()).quantidadeAtual(2).estoqueMinimo(5).build();
        Peca ok = Peca.builder().id(UUID.randomUUID()).quantidadeAtual(20).estoqueMinimo(5).build();

        when(pecaRepository.findAll()).thenReturn(List.of(baixo, ok));

        List<Peca> resultado = pecaService.listar(false);

        assertThat(resultado).hasSize(2);
    }
}
