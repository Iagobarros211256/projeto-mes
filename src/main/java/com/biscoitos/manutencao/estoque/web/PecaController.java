package com.biscoitos.manutencao.estoque.web;

import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.security.UsuarioLogadoResolver;
import com.biscoitos.manutencao.estoque.domain.MovimentacaoEstoque;
import com.biscoitos.manutencao.estoque.domain.Peca;
import com.biscoitos.manutencao.estoque.service.PecaService;
import com.biscoitos.manutencao.estoque.web.dto.CriarPecaRequest;
import com.biscoitos.manutencao.estoque.web.dto.MovimentacaoEstoqueResponse;
import com.biscoitos.manutencao.estoque.web.dto.PecaResponse;
import com.biscoitos.manutencao.estoque.web.dto.RegistrarMovimentacaoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Permissões (ver tabela no backlog-sprint5.md):
 * - Cadastrar peça: GESTOR
 * - Entrada / saída: GESTOR, TECNICO
 * - Consultar: qualquer autenticado
 */
@RestController
@RequestMapping("/api/pecas")
@RequiredArgsConstructor
public class PecaController {

    private final PecaService pecaService;
    private final UsuarioLogadoResolver usuarioLogadoResolver;

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<PecaResponse> criar(@Valid @RequestBody CriarPecaRequest request,
                                               Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        Peca peca = pecaService.criar(request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PecaResponse.from(peca));
    }

    @PostMapping("/{id}/entrada")
    @PreAuthorize("hasAnyRole('GESTOR', 'TECNICO')")
    public ResponseEntity<MovimentacaoEstoqueResponse> registrarEntrada(
            @PathVariable UUID id, @Valid @RequestBody RegistrarMovimentacaoRequest request,
            Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        MovimentacaoEstoque mov = pecaService.registrarEntrada(id, request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimentacaoEstoqueResponse.from(mov));
    }

    @PostMapping("/{id}/saida")
    @PreAuthorize("hasAnyRole('GESTOR', 'TECNICO')")
    public ResponseEntity<MovimentacaoEstoqueResponse> registrarSaida(
            @PathVariable UUID id, @Valid @RequestBody RegistrarMovimentacaoRequest request,
            Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        MovimentacaoEstoque mov = pecaService.registrarSaida(id, request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MovimentacaoEstoqueResponse.from(mov));
    }

    @GetMapping
    public List<PecaResponse> listar(
            @RequestParam(name = "apenasEstoqueBaixo", defaultValue = "false") boolean apenasEstoqueBaixo
    ) {
        return pecaService.listar(apenasEstoqueBaixo).stream().map(PecaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PecaResponse buscarPorId(@PathVariable UUID id) {
        return PecaResponse.from(pecaService.buscarPecaPorId(id));
    }
}
