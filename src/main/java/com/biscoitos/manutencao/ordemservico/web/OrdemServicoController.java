package com.biscoitos.manutencao.ordemservico.web;

import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.security.UsuarioLogadoResolver;
import com.biscoitos.manutencao.ordemservico.domain.OrdemServico;
import com.biscoitos.manutencao.ordemservico.domain.StatusOrdemServico;
import com.biscoitos.manutencao.ordemservico.service.OrdemServicoService;
import com.biscoitos.manutencao.ordemservico.web.dto.AbrirOrdemServicoRequest;
import com.biscoitos.manutencao.ordemservico.web.dto.AtribuirTecnicoRequest;
import com.biscoitos.manutencao.ordemservico.web.dto.ConcluirOrdemServicoRequest;
import com.biscoitos.manutencao.ordemservico.web.dto.OrdemServicoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Permissões (ver tabela no backlog-sprint3.md):
 * - Abrir: SUPERVISOR, GESTOR
 * - Atribuir técnico / cancelar: GESTOR
 * - Iniciar execução / concluir: TECNICO, GESTOR
 * - Consultar: qualquer autenticado
 */
@RestController
@RequestMapping("/api/ordens-servico")
@RequiredArgsConstructor
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;
    private final UsuarioLogadoResolver usuarioLogadoResolver;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'GESTOR')")
    public ResponseEntity<OrdemServicoResponse> abrir(@Valid @RequestBody AbrirOrdemServicoRequest request,
                                                       Authentication authentication) {
        Usuario solicitante = usuarioLogadoResolver.resolver(authentication);
        OrdemServico os = ordemServicoService.abrir(request, solicitante.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrdemServicoResponse.from(os));
    }

    @PatchMapping("/{id}/atribuir-tecnico")
    @PreAuthorize("hasRole('GESTOR')")
    public OrdemServicoResponse atribuirTecnico(@PathVariable UUID id,
                                                 @Valid @RequestBody AtribuirTecnicoRequest request,
                                                 Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        OrdemServico os = ordemServicoService.atribuirTecnico(id, request.tecnicoId(), usuario.getId());
        return OrdemServicoResponse.from(os);
    }

    @PatchMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('TECNICO', 'GESTOR')")
    public OrdemServicoResponse iniciar(@PathVariable UUID id, Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        OrdemServico os = ordemServicoService.iniciarExecucao(id, usuario.getId());
        return OrdemServicoResponse.from(os);
    }

    @PatchMapping("/{id}/concluir")
    @PreAuthorize("hasAnyRole('TECNICO', 'GESTOR')")
    public OrdemServicoResponse concluir(@PathVariable UUID id,
                                          @RequestBody ConcluirOrdemServicoRequest request,
                                          Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        OrdemServico os = ordemServicoService.concluir(id, request, usuario.getId());
        return OrdemServicoResponse.from(os);
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('GESTOR')")
    public OrdemServicoResponse cancelar(@PathVariable UUID id, Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        OrdemServico os = ordemServicoService.cancelar(id, usuario.getId());
        return OrdemServicoResponse.from(os);
    }

    @GetMapping
    public List<OrdemServicoResponse> listar(
            @RequestParam(required = false) StatusOrdemServico status,
            @RequestParam(required = false) UUID equipamentoId,
            @RequestParam(required = false) UUID tecnicoId,
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim
    ) {
        return ordemServicoService.listarComFiltros(status, equipamentoId, tecnicoId, dataInicio, dataFim)
                .stream().map(OrdemServicoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrdemServicoResponse buscarPorId(@PathVariable UUID id) {
        return OrdemServicoResponse.from(ordemServicoService.buscarPorId(id));
    }
}
