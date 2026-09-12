package com.biscoitos.manutencao.core.domain;

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

import java.util.UUID;

@Entity
@Table(name = "equipamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipamento {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linha_producao_id", nullable = false)
    private LinhaProducao linhaProducao;

    private String codigo;

    private String nome;

    private String setor;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatusEquipamento status = StatusEquipamento.ATIVO;

    /** Preparado para o módulo de Manutenção Preventiva (Fase 2). */
    @Builder.Default
    private Integer horasOperacaoAcumuladas = 0;

    /** Nullable — sem isso, o Dashboard não consegue calcular Performance (Sprint 6). */
    private Integer tempoCicloIdealSegundos;
}
