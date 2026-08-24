package com.biscoitos.manutencao.ordemservico.domain;

import com.biscoitos.manutencao.core.domain.Equipamento;
import com.biscoitos.manutencao.core.domain.Usuario;
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
@Table(name = "ordem_servico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdemServico {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    private String titulo;

    private String descricao;

    @Enumerated(EnumType.STRING)
    private TipoOrdemServico tipo;

    @Enumerated(EnumType.STRING)
    private PrioridadeOrdemServico prioridade;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusOrdemServico status = StatusOrdemServico.ABERTA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    /** Nullable — atribuído depois da abertura (US12), não na criação. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_responsavel_id")
    private Usuario tecnicoResponsavel;

    private Instant dataAbertura;

    private Instant dataInicioExecucao;

    private Instant dataConclusao;

    private String observacoesEncerramento;
}
