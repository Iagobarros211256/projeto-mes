package com.biscoitos.manutencao.core.web;

import com.biscoitos.manutencao.core.domain.LinhaProducao;
import com.biscoitos.manutencao.core.service.LinhaProducaoService;
import com.biscoitos.manutencao.core.web.dto.LinhaProducaoRequest;
import com.biscoitos.manutencao.core.web.dto.LinhaProducaoResponse;
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
@RequestMapping("/api/linhas-producao")
@RequiredArgsConstructor
public class LinhaProducaoController {

    private final LinhaProducaoService linhaProducaoService;

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<LinhaProducaoResponse> criar(@Valid @RequestBody LinhaProducaoRequest request) {
        LinhaProducao criada = linhaProducaoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(LinhaProducaoResponse.from(criada));
    }

    @GetMapping
    public List<LinhaProducaoResponse> listar() {
        return linhaProducaoService.listar().stream().map(LinhaProducaoResponse::from).toList();
    }
}
