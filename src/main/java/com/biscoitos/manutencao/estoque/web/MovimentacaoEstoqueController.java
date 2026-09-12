package com.biscoitos.manutencao.estoque.web;

import com.biscoitos.manutencao.estoque.domain.TipoMovimentacao;
import com.biscoitos.manutencao.estoque.service.PecaService;
import com.biscoitos.manutencao.estoque.web.dto.MovimentacaoEstoqueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** US26 — todos os filtros são opcionais; sem nenhum, devolve o histórico inteiro. */
@RestController
@RequestMapping("/api/movimentacoes-estoque")
@RequiredArgsConstructor
public class MovimentacaoEstoqueController {

    private final PecaService pecaService;

    @GetMapping
    public List<MovimentacaoEstoqueResponse> listar(
            @RequestParam(required = false) UUID pecaId,
            @RequestParam(required = false) TipoMovimentacao tipo,
            @RequestParam(required = false) Instant dataInicio,
            @RequestParam(required = false) Instant dataFim
    ) {
        return pecaService.listarMovimentacoesComFiltros(pecaId, tipo, dataInicio, dataFim).stream()
                .map(MovimentacaoEstoqueResponse::from)
                .toList();
    }
}
