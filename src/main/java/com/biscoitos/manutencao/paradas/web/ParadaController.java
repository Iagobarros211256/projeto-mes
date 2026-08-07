package com.biscoitos.manutencao.paradas.web;

import com.biscoitos.manutencao.core.domain.Usuario;
import com.biscoitos.manutencao.core.security.UsuarioLogadoResolver;
import com.biscoitos.manutencao.paradas.domain.Parada;
import com.biscoitos.manutencao.paradas.service.ParadaService;
import com.biscoitos.manutencao.paradas.web.dto.AbrirParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.EncerrarParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.ParadaResponse;
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
 * RN04: apenas OPERADOR, TECNICO e SUPERVISOR podem escrever (abrir/encerrar).
 * GESTOR e GERENTE têm acesso somente aos GETs (leitura).
 */
@RestController
@RequestMapping("/api/paradas")
@RequiredArgsConstructor
public class ParadaController {

    private final ParadaService paradaService;
    private final UsuarioLogadoResolver usuarioLogadoResolver;

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'TECNICO', 'SUPERVISOR')")
    public ResponseEntity<ParadaResponse> abrir(@Valid @RequestBody AbrirParadaRequest request,
                                                 Authentication authentication) {
        Usuario responsavel = usuarioLogadoResolver.resolver(authentication);
        Parada parada = paradaService.abrirParada(request, responsavel.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ParadaResponse.from(parada));
    }

    @PatchMapping("/{id}/encerrar")
    @PreAuthorize("hasAnyRole('OPERADOR', 'TECNICO', 'SUPERVISOR')")
    public ParadaResponse encerrar(@PathVariable UUID id, @RequestBody EncerrarParadaRequest request) {
        Parada parada = paradaService.encerrarParada(id, request);
        return ParadaResponse.from(parada);
    }

    /**
     * US08 — todos os filtros são opcionais; sem nenhum, devolve o histórico inteiro
     * (ordenado por data de início, mais recente primeiro).
     * Datas seguem ISO-8601, ex.: ?dataInicio=2026-07-01T00:00:00Z
     */
    @GetMapping
    public List<ParadaResponse> listar(
            @RequestParam(required = false) UUID equipamentoId,
            @RequestParam(required = false) UUID motivoId,
            @RequestParam(required = false) UUID responsavelId,
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim
    ) {
        return paradaService.listarComFiltros(equipamentoId, motivoId, responsavelId, dataInicio, dataFim)
                .stream().map(ParadaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ParadaResponse buscarPorId(@PathVariable UUID id) {
        return ParadaResponse.from(paradaService.buscarPorId(id));
    }
}
