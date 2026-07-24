package com.biscoitos.manutencao.core.web;

import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.service.EquipamentoService;
import com.biscoitos.manutencao.core.web.dto.EquipamentoRequest;
import com.biscoitos.manutencao.core.web.dto.EquipamentoResponse;
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
@RequestMapping("/api/equipamentos")
@RequiredArgsConstructor
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<EquipamentoResponse> criar(@Valid @RequestBody EquipamentoRequest request) {
        Equipamento criado = equipamentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(EquipamentoResponse.from(criado));
    }

    @GetMapping
    public List<EquipamentoResponse> listar() {
        return equipamentoService.listar().stream().map(EquipamentoResponse::from).toList();
    }
}
