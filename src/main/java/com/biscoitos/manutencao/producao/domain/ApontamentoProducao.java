package com.biscoitos.manutencao.producao.domain;

import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "apontamento_producao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApontamentoProducao {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    /** Início do período apontado (turno/janela) — RN13: precisa ser antes de dataHoraFim. */
    private Instant dataHoraInicio;

    private Instant dataHoraFim;

    private Integer quantidadeProduzida;

    /** RN12 — não pode ser maior que quantidadeProduzida. */
    private Integer quantidadeBoa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    /** Quando foi lançado no sistema — pode ser depois do turno em si. */
    private Instant dataRegistro;
}
