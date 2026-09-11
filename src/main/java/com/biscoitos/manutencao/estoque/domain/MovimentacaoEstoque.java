package com.biscoitos.manutencao.estoque.domain;

import com.biscoitos.manutencao.core.domain.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "movimentacao_estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Enumerated(EnumType.STRING)
    private TipoMovimentacao tipo;

    /** Sempre positivo (RN11) — o sinal da operação vem do `tipo`, não do valor. */
    private Integer quantidade;

    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    private Instant dataHora;

    /** Vínculo opcional e explícito com uma OS (US24) — com FK real (ver migration V7). */
    @Column(name = "ordem_servico_id")
    private UUID ordemServicoId;
}
