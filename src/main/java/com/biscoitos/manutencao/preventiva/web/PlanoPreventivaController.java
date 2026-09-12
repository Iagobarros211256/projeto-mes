package com.biscoitos.manutencao.preventiva.web;

import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.security.UsuarioLogadoResolver;
import com.biscoitos.manutencao.preventiva.domain.PlanoPreventiva;
import com.biscoitos.manutencao.preventiva.service.PlanoPreventivaService;
import com.biscoitos.manutencao.preventiva.web.dto.CriarPlanoPreventivaRequest;
import com.biscoitos.manutencao.preventiva.web.dto.PlanoPreventivaResponse;
import com.biscoitos.manutencao.preventiva.web.dto.RegistrarExecucaoRequest;
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

import java.util.List;
import java.util.UUID;

/**
 * Permissões (ver tabela no backlog-sprint4.md):
 * - Cadastrar/ativar/inativar: GESTOR
 * - Registrar execução: GESTOR, TECNICO
 * - Consultar: qualquer autenticado
 */
@RestController
@RequestMapping("/api/planos-preventiva")
@RequiredArgsConstructor
public class PlanoPreventivaController {

    private final PlanoPreventivaService planoPreventivaService;
    private final UsuarioLogadoResolver usuarioLogadoResolver;

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<PlanoPreventivaResponse> criar(@Valid @RequestBody CriarPlanoPreventivaRequest request,
                                                          Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        PlanoPreventiva plano = planoPreventivaService.criar(request, usuario.getId());
        PlanoPreventivaResponse response = PlanoPreventivaResponse.from(plano, planoPreventivaService.estaVencido(plano));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/executar")
    @PreAuthorize("hasAnyRole('GESTOR', 'TECNICO')")
    public PlanoPreventivaResponse registrarExecucao(@PathVariable UUID id,
                                                      @RequestBody RegistrarExecucaoRequest request,
                                                      Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        PlanoPreventiva plano = planoPreventivaService.registrarExecucao(id, request, usuario.getId());
        return PlanoPreventivaResponse.from(plano, planoPreventivaService.estaVencido(plano));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('GESTOR')")
    public PlanoPreventivaResponse ativar(@PathVariable UUID id, Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        PlanoPreventiva plano = planoPreventivaService.ativar(id, usuario.getId());
        return PlanoPreventivaResponse.from(plano, planoPreventivaService.estaVencido(plano));
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('GESTOR')")
    public PlanoPreventivaResponse inativar(@PathVariable UUID id, Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        PlanoPreventiva plano = planoPreventivaService.inativar(id, usuario.getId());
        return PlanoPreventivaResponse.from(plano, planoPreventivaService.estaVencido(plano));
    }

    @GetMapping
    public List<PlanoPreventivaResponse> listar(
            @RequestParam(required = false) UUID equipamentoId,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(name = "apenasVencidos", defaultValue = "false") boolean apenasVencidos
    ) {
        return planoPreventivaService.listarComFiltros(equipamentoId, ativo, apenasVencidos).stream()
                .map(plano -> PlanoPreventivaResponse.from(plano, planoPreventivaService.estaVencido(plano)))
                .toList();
    }

    @GetMapping("/{id}")
    public PlanoPreventivaResponse buscarPorId(@PathVariable UUID id) {
        PlanoPreventiva plano = planoPreventivaService.buscarPorId(id);
        return PlanoPreventivaResponse.from(plano, planoPreventivaService.estaVencido(plano));
    }
}
