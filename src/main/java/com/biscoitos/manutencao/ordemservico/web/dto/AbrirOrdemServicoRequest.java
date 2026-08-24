package com.biscoitos.manutencao.ordemservico.web.dto;

import com.biscoitos.manutencao.ordemservico.domain.PrioridadeOrdemServico;
import com.biscoitos.manutencao.ordemservico.domain.TipoOrdemServico;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * equipamentoId e paradaId são ambos opcionais aqui (não dá pra expressar "pelo menos um
 * dos dois" com @NotNull simples) — o Service valida que pelo menos um veio preenchido e
 * lança DadosInvalidosException se nenhum vier. Se paradaId vier, o equipamento é derivado
 * da própria parada (evita inconsistência entre os dois).
 */
public record AbrirOrdemServicoRequest(
        UUID equipamentoId,
        UUID paradaId,
        @NotBlank(message = "titulo é obrigatório") String titulo,
        String descricao,
        @NotNull(message = "tipo é obrigatório") TipoOrdemServico tipo,
        @NotNull(message = "prioridade é obrigatória") PrioridadeOrdemServico prioridade
) {
}
