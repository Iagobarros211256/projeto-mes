package com.biscoitos.manutencao.estoque.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "peca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Peca {

    @Id
    @GeneratedValue
    private UUID id;

    private String codigo;

    private String nome;

    private String descricao;

    private String unidadeMedida;

    /** Mantido a cada movimentação (ENTRADA/SAIDA), não recalculado a partir do histórico
     * a cada leitura — ver decisão em backlog-sprint5.md. */
    @Builder.Default
    private Integer quantidadeAtual = 0;

    @Builder.Default
    private Integer estoqueMinimo = 0;

    private Instant dataCriacao;
}
