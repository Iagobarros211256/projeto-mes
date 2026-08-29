package com.biscoitos.manutencao.preventiva.domain;

import com.biscoitos.manutencao.core.domain.Equipamento;
import jakarta.persistence.Column;
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
@Table(name = "plano_preventiva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanoPreventiva {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    private String titulo;

    private String descricao;

    /** Pelo menos um entre intervaloDias/intervaloHoras é obrigatório — validado no Service (RN08). */
    private Integer intervaloDias;

    private Integer intervaloHoras;

    private Instant dataUltimaExecucao;

    private Integer horasNaUltimaExecucao;

    /** Vínculo opcional e explícito com uma OS (US20) — sem FK, ver comentário na migration V6. */
    @Column(name = "ultima_ordem_servico_id")
    private UUID ultimaOrdemServicoId;

    @Builder.Default
    private boolean ativo = true;

    private Instant dataCriacao;
}
