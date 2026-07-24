package com.biscoitos.manutencao.paradas.domain;

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
@Table(name = "parada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parada {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private Equipamento equipamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motivo_id", nullable = false)
    private MotivoParada motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    /**
     * Nullable de propósito: o módulo de Ordens de Serviço ainda não existe (Fase 2).
     * A coluna já nasce pronta para não exigir ALTER TABLE depois (ver modelo-dados.md).
     */
    @jakarta.persistence.Column(name = "ordem_servico_id")
    private UUID ordemServicoId;

    private Instant dataHoraInicio;

    private Instant dataHoraFim;

    /** Calculado e persistido no encerramento — não é derivado em tempo de consulta (ver decisão em modelo-dados.md). */
    private Long duracaoMinutos;

    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusParada status = StatusParada.ABERTA;
}
