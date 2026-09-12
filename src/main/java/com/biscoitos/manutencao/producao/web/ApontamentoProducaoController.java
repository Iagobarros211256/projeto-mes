package com.biscoitos.manutencao.producao.web;

import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.security.UsuarioLogadoResolver;
import com.biscoitos.manutencao.producao.domain.ApontamentoProducao;
import com.biscoitos.manutencao.producao.service.ApontamentoProducaoService;
import com.biscoitos.manutencao.producao.web.dto.ApontamentoProducaoResponse;
import com.biscoitos.manutencao.producao.web.dto.RegistrarApontamentoRequest;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Permissões: registrar = OPERADOR, SUPERVISOR, GESTOR; consultar = qualquer autenticado. */
@RestController
@RequestMapping("/api/apontamentos-producao")
@RequiredArgsConstructor
public class ApontamentoProducaoController {

    private final ApontamentoProducaoService apontamentoProducaoService;
    private final UsuarioLogadoResolver usuarioLogadoResolver;

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'SUPERVISOR', 'GESTOR')")
    public ResponseEntity<ApontamentoProducaoResponse> registrar(@Valid @RequestBody RegistrarApontamentoRequest request,
                                                                  Authentication authentication) {
        Usuario usuario = usuarioLogadoResolver.resolver(authentication);
        ApontamentoProducao apontamento = apontamentoProducaoService.registrar(request, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApontamentoProducaoResponse.from(apontamento));
    }

    @GetMapping
    public List<ApontamentoProducaoResponse> listar(
            @RequestParam(required = false) UUID equipamentoId,
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim
    ) {
        return apontamentoProducaoService.listarComFiltros(equipamentoId, dataInicio, dataFim).stream()
                .map(ApontamentoProducaoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ApontamentoProducaoResponse buscarPorId(@PathVariable UUID id) {
        return ApontamentoProducaoResponse.from(apontamentoProducaoService.buscarPorId(id));
    }
}
