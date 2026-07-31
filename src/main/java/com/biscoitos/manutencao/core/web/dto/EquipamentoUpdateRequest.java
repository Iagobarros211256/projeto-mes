package com.biscoitos.manutencao.core.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Note que "codigo" não está aqui de propósito: é a chave de negócio do equipamento
 * (RF01/US02) e não deveria mudar depois de criado. Se um dia isso precisar mudar,
 * é uma decisão de produto separada, não um detalhe de implementação do PUT.
 */
public record EquipamentoUpdateRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        String setor,
        @NotNull(message = "linhaProducaoId é obrigatório") UUID linhaProducaoId
) {
}
