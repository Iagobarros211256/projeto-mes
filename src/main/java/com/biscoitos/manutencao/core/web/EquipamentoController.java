package com.biscoitos.manutencao.core.web;

import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.service.EquipamentoService;
import com.biscoitos.manutencao.core.web.dto.AtualizarHorasOperacaoRequest;
import com.biscoitos.manutencao.core.web.dto.AtualizarTempoCicloIdealRequest;
import com.biscoitos.manutencao.core.web.dto.EquipamentoRequest;
import com.biscoitos.manutencao.core.web.dto.EquipamentoResponse;
import com.biscoitos.manutencao.core.web.dto.EquipamentoUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public EquipamentoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody EquipamentoUpdateRequest request) {
        return EquipamentoResponse.from(equipamentoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('GESTOR')")
    public EquipamentoResponse inativar(@PathVariable UUID id) {
        return EquipamentoResponse.from(equipamentoService.inativar(id));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('GESTOR')")
    public EquipamentoResponse ativar(@PathVariable UUID id) {
        return EquipamentoResponse.from(equipamentoService.ativar(id));
    }

    /** US18 (Preventiva) — RN09 aplicada no Service. */
    @PatchMapping("/{id}/horas-operacao")
    @PreAuthorize("hasAnyRole('GESTOR', 'TECNICO')")
    public EquipamentoResponse atualizarHorasOperacao(@PathVariable UUID id,
                                                       @Valid @RequestBody AtualizarHorasOperacaoRequest request) {
        return EquipamentoResponse.from(equipamentoService.atualizarHorasOperacao(id, request.horas()));
    }

    /** US28 (Dashboard de OEE). */
    @PatchMapping("/{id}/tempo-ciclo-ideal")
    @PreAuthorize("hasRole('GESTOR')")
    public EquipamentoResponse atualizarTempoCicloIdeal(@PathVariable UUID id,
                                                         @Valid @RequestBody AtualizarTempoCicloIdealRequest request) {
        return EquipamentoResponse.from(equipamentoService.atualizarTempoCicloIdeal(id, request.segundos()));
    }
}
