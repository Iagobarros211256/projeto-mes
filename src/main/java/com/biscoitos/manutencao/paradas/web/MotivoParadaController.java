package com.biscoitos.manutencao.paradas.web;

import com.biscoitos.manutencao.paradas.domain.MotivoParada;
import com.biscoitos.manutencao.paradas.service.MotivoParadaService;
import com.biscoitos.manutencao.paradas.web.dto.MotivoParadaRequest;
import com.biscoitos.manutencao.paradas.web.dto.MotivoParadaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/motivos-parada")
@RequiredArgsConstructor
public class MotivoParadaController {

    private final MotivoParadaService motivoParadaService;

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<MotivoParadaResponse> criar(@Valid @RequestBody MotivoParadaRequest request) {
        MotivoParada criado = motivoParadaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MotivoParadaResponse.from(criado));
    }

    @GetMapping
    public List<MotivoParadaResponse> listar() {
        return motivoParadaService.listar().stream().map(MotivoParadaResponse::from).toList();
    }
}
